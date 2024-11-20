package by.bsu.dependency.example;

import by.bsu.dependency.annotation.Bean;
import by.bsu.dependency.annotation.Inject;

@Bean(name = "anotherBean")
public class AnotherBean {

    @Inject
    private NotBean notBean;

    public void doSomething() {
        System.out.println("Hi, I'm other bean");
    }

    public void doSomethingWithNotBean() {
        System.out.println("Trying to shake not bean...");
        notBean.doSomething();
    }
}
