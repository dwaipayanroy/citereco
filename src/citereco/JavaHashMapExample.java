/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package citereco;

import java.util.HashMap;

/**
 *
 * @author dwaipayan
 */
class Node {
    String  key;
    int     value;
}

public class JavaHashMapExample {
    HashMap<String, Node> objectHashMap;

    public JavaHashMapExample() {
        objectHashMap = new HashMap<>();        
    }

    /**
     * 
     * @return: true : 'key' is successfully inserted into the hashmap
     *          false: 'key' is already present in the hashmap
     */
    private boolean insertInHashMap(String key, Node value) {
        boolean found = false;

        if(null == searchInHashMap(key)) {
            objectHashMap.put(key, value);
            found = true;
        }
        return found;
    }

    /**
     * 
     * @return: NULL : 'key' is not present in the hashmap
     *          Node object : 'value' corresponding to 'key'
     */
    private Node searchInHashMap(String key) {
        Node value;
        value = objectHashMap.get(key);

        return value;
    }

    public static void main(String[] args) {
        
    }
}
