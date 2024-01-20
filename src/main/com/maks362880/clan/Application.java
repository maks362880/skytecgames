package com.maks362880.clan;


import com.maks362880.clan.dbhelper.DbHelper;
import com.maks362880.clan.service.ClanGoldManagement;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Application {
    public static void main(String[] args) {
        DbHelper.init();
        ClanGoldManagement clanGoldManagement = new ClanGoldManagement();
        CompletableFuture<Void> transferGoldFromUserToClan = clanGoldManagement.transferGoldFromUserToClan(1, 1, 3);
        CompletableFuture<Void> transferGoldFromUserTaskToClan = clanGoldManagement.transferGoldFromUserTaskToClan(2, 2, 2);
        CompletableFuture<Void> transferGoldFromClanToClan = clanGoldManagement.transferGoldFromClanToClan(1, 2, 3);

        try {
            CompletableFuture.allOf(transferGoldFromUserToClan, transferGoldFromUserTaskToClan, transferGoldFromClanToClan).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}