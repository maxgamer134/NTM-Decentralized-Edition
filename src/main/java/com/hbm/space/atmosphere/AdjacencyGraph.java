// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.atmosphere;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/** An undirected graph of nodes and their neighbours, as a sealed volume's blocks. */
final class AdjacencyGraph<T> {

    private final HashMap<T, HashSet<T>> adjacency = new HashMap<>();

    void add(T node, HashSet<T> neighbours) {
        if (contains(node)) return;
        adjacency.put(node, neighbours);
        for (T n : neighbours) adjacency.get(n).add(node);
    }

    boolean contains(T node) {
        return adjacency.containsKey(node);
    }

    Set<T> getKeys() {
        return adjacency.keySet();
    }

    HashSet<T> connectedTo(T node) {
        HashSet<T> seen = new HashSet<>();
        ArrayDeque<T> stack = new ArrayDeque<>();
        stack.push(node);
        seen.add(node);
        while (!stack.isEmpty()) {
            for (T next : adjacency.get(stack.pop())) {
                if (seen.add(next)) stack.push(next);
            }
        }
        return seen;
    }

    boolean doesPathExist(T from, T to) {
        HashSet<T> seen = new HashSet<>();
        ArrayDeque<T> stack = new ArrayDeque<>();
        stack.push(from);
        while (!stack.isEmpty()) {
            T node = stack.pop();
            seen.add(node);
            for (T next : adjacency.get(node)) {
                if (to.equals(next)) return true;
                if (!seen.contains(next)) stack.push(next);
            }
        }
        return false;
    }

    Collection<T> removeAllNodesConnectedTo(T node) {
        HashSet<T> nodes = connectedTo(node);
        for (T n : nodes) adjacency.remove(n);
        return nodes;
    }

    void remove(T node) {
        HashSet<T> set = adjacency.remove(node);
        if (set != null) for (T n : set) {
            HashSet<T> other = adjacency.get(n);
            if (other != null) other.remove(node);
        }
    }

    void clear() {
        adjacency.clear();
    }

    int size() {
        return adjacency.size();
    }
}
