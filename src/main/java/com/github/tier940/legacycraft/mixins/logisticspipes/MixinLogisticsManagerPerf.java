package com.github.tier940.legacycraft.mixins.logisticspipes;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.interfaces.routing.IProvideItems;
import logisticspipes.items.LogisticsFluidContainer;
import logisticspipes.logistics.LogisticsManager;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.PipeRoutingConnectionType;
import logisticspipes.routing.ServerRouter;
import logisticspipes.utils.FluidSinkReply;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.tuples.Pair;
import logisticspipes.utils.tuples.Triplet;

@Mixin(value = LogisticsManager.class, remap = false)
public abstract class MixinLogisticsManagerPerf {

    @Shadow
    private Triplet<Integer, SinkReply, List<IFilter>> getBestReply(ItemStack stack, ItemIdentifier item,
                                                                    IRouter sourceRouter,
                                                                    List<ExitRoute> validDestinations,
                                                                    boolean excludeSource, List<Integer> jamList,
                                                                    Triplet<Integer, SinkReply, List<IFilter>> result,
                                                                    boolean allowDefault) {
        throw new AssertionError();
    }

    /**
     * @author LC-Core
     * @reason Replace Stream.filter().collect() with direct loop to reduce allocations
     */
    @Overwrite
    public IRoutedItem assignDestinationFor(IRoutedItem item, int sourceRouterID, boolean excludeSource) {
        Pair<Integer, FluidSinkReply> bestReply;
        ServerRouter sourceRouter = SimpleServiceLocator.routerManager.getServerRouter(sourceRouterID);
        if (sourceRouter == null) {
            return item;
        }
        item.clearDestination();
        ItemIdentifierStack itemIdStack = item.getItemIdentifierStack();
        if (itemIdStack == null) {
            return item;
        }
        BitSet routersIndex = ServerRouter.getRoutersInterestedIn(itemIdStack.getItem());
        ArrayList<ExitRoute> validDestinations = new ArrayList<>(routersIndex.cardinality());
        for (int i = routersIndex.nextSetBit(0); i >= 0; i = routersIndex.nextSetBit(i + 1)) {
            ServerRouter r = SimpleServiceLocator.routerManager.getServerRouter(i);
            List<ExitRoute> exits = sourceRouter.getDistanceTo(r);
            if (exits != null) {
                for (ExitRoute e : exits) {
                    if (e.containsFlag(PipeRoutingConnectionType.canRouteTo)) {
                        validDestinations.add(e);
                    }
                }
            }
        }
        Collections.sort(validDestinations);
        ItemStack stack = itemIdStack.makeNormalStack();
        if (stack.getItem() instanceof LogisticsFluidContainer) {
            bestReply = SimpleServiceLocator.logisticsFluidManager.getBestReply(
                    SimpleServiceLocator.logisticsFluidManager.getFluidFromContainer(itemIdStack), sourceRouter,
                    item.getJamList());
            if (bestReply != null) {
                item.setDestination(bestReply.getValue1());
            }
        } else {
            Triplet<Integer, SinkReply, List<IFilter>> reply2 = getBestReply(stack, itemIdStack.getItem(), sourceRouter,
                    validDestinations, excludeSource, item.getJamList(), null, true);
            if (reply2.getValue1() != null && reply2.getValue1() != 0) {
                item.setDestination(reply2.getValue1());
                SinkReply reply = reply2.getValue2();
                if (reply.isPassive) {
                    if (reply.isDefault) {
                        item.setTransportMode(IRoutedItem.TransportMode.Default);
                    } else {
                        item.setTransportMode(IRoutedItem.TransportMode.Passive);
                    }
                } else {
                    item.setTransportMode(IRoutedItem.TransportMode.Active);
                }
                item.setAdditionalTargetInformation(reply.addInfo);
            }
        }
        return item;
    }

    /**
     * @author LC-Core
     * @reason Avoid allocating getBiggestSimpleID() HashMaps per call
     */
    @Overwrite
    public HashMap<ItemIdentifier, Integer> getAvailableItems(List<ExitRoute> validDestinations) {
        HashMap<ItemIdentifier, Integer> allAvailableItems = new HashMap<>();
        BitSet used = new BitSet(ServerRouter.getBiggestSimpleID());
        outer:
        for (ExitRoute r : validDestinations) {
            if (r == null || !r.containsFlag(PipeRoutingConnectionType.canRequestFrom) ||
                    !(r.destination.getPipe() instanceof IProvideItems iProvideItems)) {
                continue;
            }
            int simpleID = r.destination.getSimpleID();
            if (used.get(simpleID)) {
                continue;
            }
            for (IFilter filter : r.filters) {
                if (filter.blockProvider()) {
                    continue outer;
                }
            }
            HashMap<ItemIdentifier, Integer> routerItems = new HashMap<>();
            iProvideItems.getAllItems(routerItems, r.filters);
            for (Map.Entry<ItemIdentifier, Integer> entry : routerItems.entrySet()) {
                allAvailableItems.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
            used.set(simpleID, true);
        }
        return allAvailableItems;
    }

    /**
     * @author LC-Core
     * @reason Avoid allocating getBiggestSimpleID() HashMaps per call
     */
    @Overwrite
    public int getAmountFor(ItemIdentifier itemType, List<ExitRoute> validDestinations) {
        int amount = 0;
        BitSet used = new BitSet(ServerRouter.getBiggestSimpleID());
        outer:
        for (ExitRoute r : validDestinations) {
            if (r == null || !r.containsFlag(PipeRoutingConnectionType.canRequestFrom) ||
                    !(r.destination.getPipe() instanceof IProvideItems iProvideItems)) {
                continue;
            }
            int simpleID = r.destination.getSimpleID();
            if (used.get(simpleID)) {
                continue;
            }
            for (IFilter filter : r.filters) {
                if (filter.blockProvider()) {
                    continue outer;
                }
            }
            HashMap<ItemIdentifier, Integer> routerItems = new HashMap<>();
            iProvideItems.getAllItems(routerItems, r.filters);
            Integer count = routerItems.get(itemType);
            if (count != null) {
                amount += count;
            }
            used.set(simpleID, true);
        }
        return amount;
    }
}
