package com.github.tier940.legacycraft.core.logisticspipes;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.request.resources.IResource;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.ServerRouter;

public final class RequestTreeNodeHelper {

    private static final Map<IResource, List<ExitRoute>> CACHE = new WeakHashMap<>();

    private RequestTreeNodeHelper() {}

    public static List<ExitRoute> buildValidSources(IResource requestType) {
        List<ExitRoute> cached = CACHE.get(requestType);
        if (cached != null) {
            return new ArrayList<>(cached);
        }
        BitSet routersIndex = ServerRouter.getRoutersInterestedIn(requestType);
        ArrayList<ExitRoute> validSources = new ArrayList<>();
        for (int i = routersIndex.nextSetBit(0); i >= 0; i = routersIndex.nextSetBit(i + 1)) {
            ServerRouter r = SimpleServiceLocator.routerManager.getServerRouter(i);
            if (r == null || r.isCacheInvalid()) continue;
            List<ExitRoute> e = requestType.getRouter().getDistanceTo(r);
            if (e != null) {
                validSources.addAll(e);
            }
        }
        CACHE.put(requestType, new ArrayList<>(validSources));
        return validSources;
    }
}
