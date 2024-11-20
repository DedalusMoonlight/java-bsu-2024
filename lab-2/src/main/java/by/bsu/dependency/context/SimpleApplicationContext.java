package by.bsu.dependency.context;

import by.bsu.dependency.annotation.Bean;
import by.bsu.dependency.annotation.BeanScope;
import by.bsu.dependency.annotation.Inject;
import by.bsu.dependency.exceptions.ApplicationContextNotStartedException;
import by.bsu.dependency.exceptions.NoSuchBeanDefinitionException;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SimpleApplicationContext extends AbstractApplicationContext {
    private final Map<String, Class<?>> beanDefinitions;
    private final Map<String, Object> beans = new HashMap<>();
    private Object singleton;

    /**
     * Создает контекст, содержащий классы, переданные в параметре.
     * <br/>
     * Если на классе нет аннотации {@code @Bean}, имя бина получается из названия класса, скоуп бина по дефолту
     * считается {@code Singleton}.
     * <br/>
     * Подразумевается, что у всех классов, переданных в списке, есть конструктор без аргументов.
     *
     * @param beanClasses классы, из которых требуется создать бины
     */
    public SimpleApplicationContext(Class<?>... beanClasses) {
        this.beanDefinitions = Arrays.stream(beanClasses).collect(
                Collectors.toMap(
                        this::GetName,
                        Function.identity()));
//        System.out.println(beanDefinitions);
    }

    private <T> String GetName(Class<T> clazz) {
        return clazz.getAnnotation(Bean.class) == null
                ? Arrays.stream(clazz.getName().split("\\.")).toList().getLast()
                .transform(s -> s.substring(0, 1).toLowerCase() + s.substring(1))
                : clazz.getAnnotation(Bean.class).name();
    }

    /**
     * Помимо прочего, метод должен заниматься внедрением зависимостей в создаваемые объекты
     */
    @Override
    public void start() {
        beanDefinitions.forEach((beanName, beanClass) -> {
            if (beanClass.getAnnotation(Bean.class) == null || beanClass.getAnnotation(Bean.class).scope() == BeanScope.SINGLETON) {
                beans.put(beanName, instantiateBean(beanClass));
                InjectAll(beanClass.cast(beans.get(beanName)));
            }
        });

    }

    private void InjectAll(Object obj) {
        Arrays.stream(obj.getClass().getDeclaredFields()).forEach(field -> {
            if (field.isAnnotationPresent(Inject.class)) {
                Object injecting = instantiateBean(field.getType());
                try {
                    field.setAccessible(true);
                    field.set(obj, injecting);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
//                        beans.replace(beanName, singleton);
            }
        });
    }

    @Override
    public boolean isRunning() {
        return !beans.isEmpty();
    }

    @Override
    public boolean containsBean(String name) {
        if (!isRunning()) {
            throw new ApplicationContextNotStartedException("");
        }
        return beanDefinitions.containsKey(name);
    }

    @Override
    public Object getBean(String name) {
        if (!isRunning()) {
            throw new ApplicationContextNotStartedException("");
        }
        if (beanDefinitions.get(name) == null) {
            throw new NoSuchBeanDefinitionException("");
        }
        if (isSingleton(name)) {
            return beans.get(name);
        }
        Object obj = instantiateBean(beanDefinitions.get(name));
        InjectAll(beanDefinitions.get(name).cast(obj));
        return obj;
    }

    @Override
    public <T> T getBean(Class<T> clazz) {
        if (!isRunning()) {
            throw new ApplicationContextNotStartedException("");
        }
        String name = GetName(clazz);
        return clazz.cast(getBean(name));
    }

    @Override
    public boolean isPrototype(String name) {
        if (beanDefinitions.get(name) == null) {
            throw new NoSuchBeanDefinitionException("");
        }
        var beanClass = beanDefinitions.get(name);
        if (beanClass.getAnnotation(Bean.class) == null) {
            return false;
        }
        return beanClass.getAnnotation(Bean.class).scope() == BeanScope.PROTOTYPE;
    }

    @Override
    public boolean isSingleton(String name) {
        if (beanDefinitions.get(name) == null) {
            throw new NoSuchBeanDefinitionException("");
        }
        var beanClass = beanDefinitions.get(name);
        if (beanClass.getAnnotation(Bean.class) == null) {
            return true;
        }
        return beanClass.getAnnotation(Bean.class).scope() == BeanScope.SINGLETON;
    }

    private <T> T instantiateBean(Class<T> beanClass) {
        try {
            return beanClass.getConstructor().newInstance();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException |
                 InstantiationException e) {
            throw new RuntimeException(e);
        }
    }
}
