package models;

import gg.ingot.iron.annotations.Model;

@SuppressWarnings({"MissingJavadoc", "BooleanMethodNameMustStartWithQuestion"})
@Model
public class UserClass {
    /**
     * The name of the user
     */
    private String name;
    /**
     * The age of the user
     */
    private int age;
    /**
     * The age of the user
     */
    private boolean active;
    
    public UserClass(String name, int age, boolean active) {
        this.name = name;
        this.age = age;
        this.active = active;
    }
    
    public String name() {
        return name;
    }
    
    public UserClass setName(String name) {
        this.name = name;
        return this;
    }
    
    public int age() {
        return age;
    }
    
    public UserClass setAge(int age) {
        this.age = age;
        return this;
    }
    
    public boolean active() {
        return active;
    }
    
    public UserClass setActive(boolean active) {
        this.active = active;
        return this;
    }
}