package com.pamirs.attach.plugin.apache.kafka.origin.selector;

import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Random;

public class RecordsRatioPollSelector implements PollConsumerSelector {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private int ratioPoint;

    private Random random = new Random();

    private PollConsumerSelector selector = new PollingSelector();

    private Deque<Integer> bizRecordCounts = new LinkedList();
    private Deque<Integer> ptRecordCounts = new LinkedList();

    private static final String TEST_MODE_POLL = "agent.test.mode.poll";

    private static boolean testModePoll = System.getProperty(TEST_MODE_POLL) != null;

    private int bound = 100;

    public RecordsRatioPollSelector() {
        this.startRatioComputingTask();
    }

    public void addBizRecordCounts(int count) {
        if (bizRecordCounts.size() > 5) {
            bizRecordCounts.removeLast();
        }
        bizRecordCounts.addFirst(count);
    }

    public void addPtRecordCounts(int count) {
        if (ptRecordCounts.size() > 5) {
            ptRecordCounts.removeLast();
        }
        ptRecordCounts.addFirst(count);
    }

    @Override
    public ConsumerType select() {
        // 最开始时业务影子各一次, 业务压测消息个数一样时各一次
        if (testModePoll) {
            return selector.select();
        }
        // 1～100
        int i = random.nextInt(bound) + 1;
        return i >= ratioPoint ? ConsumerType.BIZ : ConsumerType.SHADOW;
    }

    private int sumRecordCounts(Deque<Integer> deque) {
        int count = 0;
        for (Integer integer : new ArrayList<Integer>(deque)) {
            if(integer == null){
                continue;
            }
            count += integer;
        }
        return count;
    }

    private void startRatioComputingTask() {

        new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        RecordsRatioPollSelector.this.computeRatio();
                        Thread.sleep(10 * 1000);
                    } catch (InterruptedException e) {

                    }
                }
            }
        }.start();
    }

    /**
     * 计算业务和压测拉取到的消息的比例, 0～100之间
     */
    private void computeRatio() {
        double maxPtRatioPoint = GlobalConfig.getInstance().getSimulatorDynamicConfig().getKafkaPtConsumerPollMaxRatio();
        int bizRecords = sumRecordCounts(bizRecordCounts);
        int ptRecords = sumRecordCounts(ptRecordCounts);

        // 相等取0
        if (bizRecords == ptRecords) {
            this.ratioPoint = (int) (bound * (0.5 > maxPtRatioPoint ? maxPtRatioPoint : 0.5));
            return;
        }
        // 1~20 属于压测;  21~100 属于业务
        if (bizRecords > 0 && ptRecords == 0) {
            double ratio = 0.2 > maxPtRatioPoint ? maxPtRatioPoint : 0.2;
            this.ratioPoint = (int) (bound * ratio);
            return;
        }
        //  1~80 属于压测; 81~100属于业务
        if (bizRecords == 0 && ptRecords > 0) {
            double ratio = 0.8 > maxPtRatioPoint ? maxPtRatioPoint : 0.8;
            this.ratioPoint = (int) (bound * ratio);
            return;
        }
        boolean bizMore = bizRecords >= ptRecords;
        double ratio = bizMore ? 1.0 * bizRecords / ptRecords : 1.0 * ptRecords / bizRecords;
        // 比例最大4,默认值
        ratio = ratio > 4.0 ? 4.0 : ratio;
        // 假设比例3， 100 * 3/4 = 75
        int point = (int) (bound * ratio / (ratio + 1));
        // 10 -7 + 1 = 4
        point = bizMore ? bound - point + 1 : point + 1;
        this.ratioPoint = point > bound * maxPtRatioPoint ? (int) (bound * maxPtRatioPoint) : point;
        logger.info("[SIMULATOR KAFKA] kafka consumer poll ratio point: " + ratioPoint);
    }

}
