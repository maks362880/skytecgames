package com.maks362880.clan.model;

import java.util.Objects;

public class Users {
    private long id;
    private String name;
    private int gold;

    public Users(String name, int gold) {
        this.name = name;
        this.gold = gold;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getGold() {
        return gold;
    }

    public void setGold(int gold) {
        this.gold = gold;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Users users = (Users) o;
        return id == users.id && gold == users.gold && Objects.equals(name, users.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, gold);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", gold=" + gold +
                '}';
    }
}