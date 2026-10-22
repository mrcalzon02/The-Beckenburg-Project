package org.beckenburg;

import java.util.*;

/** Room-mode integration fixture: traverse the actual authored portal graph. */
public final class LanTestRoomTravel {
    @FunctionalInterface public interface Ack { void await(String prefix) throws Exception; }
    private static final Map<LanClient,Integer> rooms = Collections.synchronizedMap(new WeakHashMap<>());
    private LanTestRoomTravel() {}
    public static void move(LanClient client, Ack ack, int target) throws Exception {
        Objects.requireNonNull(client); Objects.requireNonNull(ack);
        HouseMap map = new HouseMap();
        int start = rooms.getOrDefault(client, HouseMap.EXTRACTION_ROOM);
        if (start == target) return;
        int[] previous = new int[HouseMap.ROOM_COUNT];
        Arrays.fill(previous, -1);
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(start); previous[start] = start;
        while (!queue.isEmpty() && previous[target] == -1) {
            int current = queue.removeFirst();
            for (int next : map.adjacentRooms(current)) {
                if (previous[next] != -1) continue;
                previous[next] = current; queue.addLast(next);
            }
        }
        if (previous[target] == -1)
            throw new AssertionError("No authored room route from " + start + " to " + target);
        ArrayDeque<Integer> route = new ArrayDeque<>();
        for (int at = target; at != start; at = previous[at]) route.addFirst(at);
        for (int at : route) {
            client.move(at);
            ack.await("ACCEPT|MOVE|" + at);
            rooms.put(client, at);
        }
    }
}
