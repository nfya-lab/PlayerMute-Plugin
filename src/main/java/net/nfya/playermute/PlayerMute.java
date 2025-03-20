package net.nfya.playermute;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class PlayerMute extends JavaPlugin implements Listener {
    private final Set<String> mutedPlayers = new HashSet<>();
    private final File muteFile = new File(getDataFolder(), "list.txt");
    private long lastModifiedTime = 0;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        loadMutedPlayers();

        // 5秒ごとにファイルの変更をチェック
        new BukkitRunnable() {
            @Override
            public void run() {
                checkFileUpdate();
            }
        }.runTaskTimer(this, 100L, 100L); // 100L = 5秒
    }

    @Override
    public void onDisable() {
        saveMutedPlayers();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        if (mutedPlayers.contains(player.getName())) {
            // コンソールには [ミュート] <username> message の形式で出力
            getLogger().info("[ミュート] <" + player.getName() + "> " + message);

            // 他のプレイヤーに送信されないようにする（ただしプレイヤー自身は影響なし）
            event.getRecipients().removeIf(p -> !p.equals(player));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "使用方法: /" + label + " <プレイヤー名>");
            return true;
        }

        String playerName = args[0];

        if (command.getName().equalsIgnoreCase("mute")) {
            if (mutedPlayers.add(playerName)) {
                sender.sendMessage(ChatColor.GREEN + playerName + " をミュートしました。");
                saveMutedPlayers();
            } else {
                sender.sendMessage(ChatColor.RED + playerName + " は既にミュートされています。");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("unmute")) {
            if (mutedPlayers.remove(playerName)) {
                sender.sendMessage(ChatColor.GREEN + playerName + " のミュートを解除しました。");
                saveMutedPlayers();
            } else {
                sender.sendMessage(ChatColor.RED + playerName + " はミュートされていません。");
            }
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            if (command.getName().equalsIgnoreCase("mute")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (command.getName().equalsIgnoreCase("unmute")) {
                return mutedPlayers.stream()
                        .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return null;
    }

    private void loadMutedPlayers() {
        if (!muteFile.exists()) {
            try {
                getDataFolder().mkdirs();
                muteFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("ミュートリストのファイルを作成できませんでした！");
                e.printStackTrace();
            }
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(muteFile))) {
            mutedPlayers.clear();
            String line;
            while ((line = reader.readLine()) != null) {
                mutedPlayers.add(line.trim());
            }
            lastModifiedTime = muteFile.lastModified();
            getLogger().info("ミュートリストを読み込みました (" + mutedPlayers.size() + " 人)");
        } catch (IOException e) {
            getLogger().severe("ミュートリストの読み込みに失敗しました！");
            e.printStackTrace();
        }
    }

    private void saveMutedPlayers() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(muteFile))) {
            for (String player : mutedPlayers) {
                writer.write(player);
                writer.newLine();
            }
            lastModifiedTime = muteFile.lastModified();
            getLogger().info("ミュートリストを保存しました (" + mutedPlayers.size() + " 人)");
        } catch (IOException e) {
            getLogger().severe("ミュートリストの保存に失敗しました！");
            e.printStackTrace();
        }
    }

    private void checkFileUpdate() {
        if (muteFile.exists() && muteFile.lastModified() > lastModifiedTime) {
            getLogger().info("ミュートリストが更新されたため再読み込みします...");
            loadMutedPlayers();
        }
    }
}
