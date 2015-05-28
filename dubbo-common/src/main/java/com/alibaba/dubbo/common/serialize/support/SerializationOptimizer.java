package com.alibaba.dubbo.common.serialize.support;

import java.util.Collection;

/**
 * This class can be replaced with the contents in config file, but for now I think the class is easier to write
 *
 * @author lishen
 */
public interface SerializationOptimizer {

    public static final int CLASS_ID_BASE_START = 10000000;

    //public int sepStart();

    Collection<Class> getSerializableClasses();

    /*default public int seqNext(int idx) {
        return CLASS_ID_BASE_START + sepStart() + idx;
    }*/
}
