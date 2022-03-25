//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package swiftsdk;

import swiftsdk.util.TokenCache;

public class SFOSSObjectManager {
    private static volatile SFOSSObjectManager sfossObjectManager;
    private static TokenCache tokenCache;

    public SFOSSObjectManager(TokenCache tokenCache) {
        SFOSSObjectManager.tokenCache = tokenCache;
    }

    public static SFOSSObjectManager getInstance(TokenCache tokenCache) {
        if (sfossObjectManager == null) {
            Class var1 = SFOSSObjectManager.class;
            synchronized(SFOSSObjectManager.class) {
                if (sfossObjectManager == null) {
                    sfossObjectManager = new SFOSSObjectManager(tokenCache);
                }
            }
        }

        return sfossObjectManager;
    }

    public static TokenCache getTokenCache() {
        return tokenCache;
    }
}
