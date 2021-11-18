package org.codeplay.examples;

import org.codeplay.annotations.Factory;

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
