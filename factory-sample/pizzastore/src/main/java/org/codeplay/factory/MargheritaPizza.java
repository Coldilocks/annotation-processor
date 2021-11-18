package org.codeplay.factory;


import org.codeplay.factory.annotation.Factory;

/**
 * @author coldilock
 */
@Factory(id = "Margherita", type = Meal.class)
public class MargheritaPizza implements Meal{
    @Override
    public float getPrice() {
        return 6f;
    }
}
