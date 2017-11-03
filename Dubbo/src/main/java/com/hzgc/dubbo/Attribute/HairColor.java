package com.hzgc.dubbo.Attribute;


import java.io.Serializable;

/**
 * 头发颜色：0->无；1->金色；2->黑色；3->棕色；4->灰白
 */
public enum HairColor implements Serializable {
    None, Blond, Black, Brown, Gray;

    private String value;

    /**
     * 与其他属性的拼接运算，默认是OR运算
     */
    private Logistic logistic = Logistic.OR;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Logistic getLogistic() {
        return logistic;
    }

    public void setLogistic(Logistic logistic) {
        this.logistic = logistic;
    }

    @Override
    public String toString() {
        return "Eyeglasses{" +
                "value='" + value + '\'' +
                ", logistic=" + logistic +
                '}';
    }
}
