package com.alibaba.dubbo.common.serialize.support;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author lishen
 */
public abstract class SerializableClassRegistry {

    //modify by lubin.xuan
    private static final ZkClassIdGenerate idGenerate;

    static {
        //modify by lubin.xuan
        try {
            idGenerate = new ZkClassIdGenerate();
        } catch (Exception e) {
            throw new Error("Class ID 生成器 异常!!!");
        }
        //modify by lubin.xuan
    }

    public static class ClassId {
        private Class clazz;
        private int id;

        public ClassId() {
        }

        public ClassId(Class clazz, int id) {
            this.clazz = clazz;
            this.id = id;
        }

        public Class getClazz() {
            return clazz;
        }

        public int getId() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ClassId classId = (ClassId) o;

            return !(clazz != null ? !clazz.equals(classId.clazz) : classId.clazz != null);

        }

        @Override
        public int hashCode() {
            return clazz != null ? clazz.hashCode() : 0;
        }
    }

    private static final Set<ClassId> registrations = new LinkedHashSet<>();
    private static final Set<String> clazzFilter = new LinkedHashSet<>();

    public static void lock() {
        idGenerate.lock();
    }

    public static void unlock() {
        idGenerate.unlock();
    }


    /**
     * only supposed to be called at startup time
     */
    public static void registerClass(Class clazz) {
        if (clazzFilter.add(clazz.getName())) {
            registrations.add(new ClassId(clazz, idGenerate.getId(clazz)));
        }
    }

    public static Integer tryFind(Class clazz) {
        return idGenerate.tryFind(clazz);
    }

    public static void registerClass(Class clazz, int id) {
        if (clazzFilter.add(clazz.getName())) {
            registrations.add(new ClassId(clazz, id));
        }
    }

    public static Set<ClassId> getRegisteredClasses() {
        return registrations;
    }
}
