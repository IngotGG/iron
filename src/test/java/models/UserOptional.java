package models;

import gg.ingot.iron.annotations.Model;

import java.util.Optional;

@SuppressWarnings({"MissingJavadoc", "OptionalUsedAsFieldOrParameterType", "FieldHasSetterButNoGetter"})
@Model
public class UserOptional {
    private String name;
    private Optional<Integer> age;
    private boolean active;
    
    public UserOptional(String name, Optional<Integer> age, boolean active) {
        this.name = name;
        this.age = age;
        this.active = active;
    }
    
    public UserOptional() {}
    
    public String name() {
        return name;
    }
    
    public Optional<Integer> age() {
        return age;
    }
    
    public boolean active() {
        return active;
    }
}