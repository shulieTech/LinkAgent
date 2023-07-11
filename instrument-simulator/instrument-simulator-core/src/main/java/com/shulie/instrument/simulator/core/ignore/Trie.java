package com.shulie.instrument.simulator.core.ignore;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

public class Trie<V> {

    private final Node<V> root;

    private Trie(Node<V> root) {
        this.root = root;
    }

    public V getOrNull(CharSequence str) {
        return getOrDefault(str, null);
    }

    public V getOrDefault(CharSequence str, V defaultValue) {
        Node<V> node = root;
        V lastMatchedValue = defaultValue;

        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            Node<V> next = node.getNext(c);
            if (next == null) {
                return lastMatchedValue;
            }
            node = next;
            // next node matched, use its value if it's defined
            lastMatchedValue = next.value != null ? next.value : lastMatchedValue;
        }

        return lastMatchedValue;
    }

    static final class Node<V> {
        final char[] chars;
        final Node<V>[] children;
        final V value;

        Node(char[] chars, Node<V>[] children, V value) {
            this.chars = chars;
            this.children = children;
            this.value = value;
        }

        @Nullable
        Node<V> getNext(char c) {
            int index = Arrays.binarySearch(chars, c);
            if (index < 0) {
                return null;
            }
            return children[index];
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder<V> {

        private final NodeBuilder<V> root = new NodeBuilder<V>();

        public Builder<V> put(CharSequence str, V value) {
            put(root, str, 0, value);
            return this;
        }

        private void put(NodeBuilder<V> node, CharSequence str, int i, V value) {
            if (str.length() == i) {
                node.value = value;
                return;
            }
            char c = str.charAt(i);
            NodeBuilder<V> next = node.children.computeIfAbsent(c, new Function<Character, NodeBuilder<V>>() {
                @Override
                public NodeBuilder<V> apply(Character character) {
                    return new NodeBuilder<V>();
                }
            });
            put(next, str, i + 1, value);
        }

        public Trie<V> build() {
            return new Trie<V>(root.build());
        }
    }

    public static class NodeBuilder<V> {
        final Map<Character, NodeBuilder<V>> children = new HashMap<Character, NodeBuilder<V>>();
        V value;

        Node<V> build() {
            int size = children.size();
            char[] chars = new char[size];
            @SuppressWarnings({"unchecked", "rawtypes"})
            Node<V>[] nodes = new Node[size];

            int i = 0;

            List<Map.Entry<Character, NodeBuilder<V>>> entries = new ArrayList<Map.Entry<Character, NodeBuilder<V>>>(this.children.entrySet());
            Collections.sort(entries, comparingByKey());
            Iterator<Map.Entry<Character, NodeBuilder<V>>> it = entries.iterator();

            while (it.hasNext()) {
                Map.Entry<Character, NodeBuilder<V>> e = it.next();
                chars[i] = e.getKey();
                nodes[i++] = e.getValue().build();
            }

            return new Node<V>(chars, nodes, value);
        }
    }

    public static Comparator<Map.Entry<Character, ?>> comparingByKey() {
        return new Comparator<Map.Entry<Character, ?>>() {
            @Override
            public int compare(Map.Entry<Character, ?> c1, Map.Entry<Character, ?> c2) {
                return c1.getKey().compareTo(c2.getKey());
            }
        };
    }
}
