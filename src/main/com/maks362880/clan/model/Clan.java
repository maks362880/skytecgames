package com.maks362880.clan.model;

import java.util.Objects;

public class Clan {
    private long id;
    private String name;
    private int gold;

    public Clan() {
    }

    public Clan(String name, int gold) {
        this.name = name;
        this.gold = gold;
    }

    // Геттеры и сеттеры

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
        Clan clan = (Clan) o;
        return id == clan.id && gold == clan.gold && Objects.equals(name, clan.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, gold);
    }

    @Override
    public String toString() {
        return "Clan{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", gold=" + gold +
                '}';
    }
}