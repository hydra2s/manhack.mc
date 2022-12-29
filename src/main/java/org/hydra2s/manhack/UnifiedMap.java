package org.hydra2s.manhack;

import org.hydra2s.noire.objects.PipelineLayoutObj;

//
import java.util.HashMap;

//
public class UnifiedMap <E> {
    public PipelineLayoutObj.OutstandingArray<E> arrayMap = new PipelineLayoutObj.OutstandingArray<E>();
    public HashMap<Integer, E> hashMap = new HashMap<>();

    public UnifiedMap() {
        this.arrayMap = new PipelineLayoutObj.OutstandingArray<E>();
        this.hashMap = new HashMap<>();
    }

    public void put(int i, E obj) {
        hashMap.put(i, obj);
    }

    public int push(E obj) {
        return arrayMap.push(obj);
    }

    // TODO: fix issues
    public void removeMem(E obj) {
        arrayMap.removeMem(obj);
        hashMap.remove(obj);
    }

    // TODO: fix issues
    public void removeIndex(int i) {
        arrayMap.removeIndex(i);
        hashMap.remove(i);
    }

    public E get(int i) {
        return hashMap.containsKey(i) ? hashMap.get(i) : arrayMap.get(i);
    }

    public boolean contains(E obj) {
        return arrayMap.contains(obj);
    }
}
