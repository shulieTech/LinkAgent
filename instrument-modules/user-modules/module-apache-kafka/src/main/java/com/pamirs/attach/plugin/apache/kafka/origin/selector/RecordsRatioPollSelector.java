package com.pamirs.attach.plugin.apache.kafka.origin.selector;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Random;

public class RecordsRatioPollSelector implements PollConsumerSelector {

    private int ratioPoint;

    private Random random = new Random();

    private PollConsumerSelector selector = new PollingSelector();

    private Deque<Integer> bizRecordCounts = new LinkedList();
    private Deque<Integer> ptRecordCounts = new LinkedList();

    private static final String AGENT_TEST_MODE = "agent.test.mode.poll";

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
        if (ratioPoint == 0 || System.getProperty(AGENT_TEST_MODE) != null) {
            return selector.select();
        }
        // 1～10
        int i = random.nextInt(10) + 1;
        return i >= ratioPoint ? ConsumerType.BIZ : ConsumerType.SHADOW;
    }

    private int sumRecordCounts(Deque<Integer> deque) {
        int count = 0;
        for (Integer integer : new ArrayList<Integer>(deque)) {
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
                        if (bizRecordCounts.size() >= 5 && ptRecordCounts.size() >= 5) {
                            RecordsRatioPollSelector.this.computeRatio();
                        }
                        Thread.sleep(10 * 1000);
                    } catch (InterruptedException e) {

                    }
                }
            }
        }.start();
    }


    /**
     * 计算业务和压测拉取到的消息的比例, 0～10之间
     */
    private void computeRatio() {
        int bizRecords = sumRecordCounts(bizRecordCounts);
        int ptRecords = sumRecordCounts(ptRecordCounts);

        // 相等取0
        if (bizRecords == ptRecords) {
            this.ratioPoint = 0;
            return;
        }
        // 1 2 属于压测;  3 4 5 6 7 8 9 10 属于业务
        if (bizRecords > 0 && ptRecords == 0) {
            this.ratioPoint = 3;
            return;
        }
        //  1 2 3 4 5 6 7 8 属于压测; 9 10属于业务
        if (bizRecords == 0 && ptRecords > 0) {
            this.ratioPoint = 9;
            return;
        }

        boolean bizMore = bizRecords >= ptRecords;
        int ratio = bizMore ? bizRecords / ptRecords : ptRecords / bizRecords;
        // 比例最大4
        ratio = ratio > 4 ? 4 : ratio;
        // 假设比例3， 10 * 3/4 = 7
        ratio = 10 * ratio / (ratio + 1);
        // 10 -7 + 1 = 4
        this.ratioPoint = bizMore ? 10 - ratio + 1 : ratio + 1;
    }

}
