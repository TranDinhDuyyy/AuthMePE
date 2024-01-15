package me.shinthl09.event;

import fr.xephi.authme.api.v3.AuthMeApi;
import me.shinthl09.AuthMePE;
import me.shinthl09.color.color;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.geysermc.cumulus.CustomForm;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.util.FormBuilder;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.cumulus.util.glue.CustomFormGlue;
import org.geysermc.geyser.api.GeyserApi;

public class PlayerJoin implements Listener {
    private final Plugin plugin = AuthMePE.getPlugin(AuthMePE.class);

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        new BukkitRunnable() {
            public void run() {
                handlePlayerJoin(event.getPlayer());
            }
        }.runTaskLater(plugin, plugin.getConfig().getLong("Setting.Delay-Open-Menu") * 10L);
    }

    private void handlePlayerJoin(Player player) {
        if (GeyserApi.api().isBedrockPlayer(player.getUniqueId())) {
            if (AuthMeApi.getInstance().isRegistered(player.getName())) {
                handleRegisteredPlayer(player);
            } else {
                handleUnregisteredPlayer(player);
            }
        }
    }

    private void handleRegisteredPlayer(Player player) {
        if (!AuthMeApi.getInstance().isAuthenticated(player)) {
            if (!plugin.getConfig().getBoolean("Setting.Menu-AuthMe-Geyser")) {
                player.sendMessage(color.transalate(plugin.getConfig().getString("Message.Auto-Login")));
                AuthMeApi.getInstance().forceLogin(player);
            } else {
                sendLoginMenu(player, null);
            }
        }
    }

    private void handleUnregisteredPlayer(Player player) {
        if (!plugin.getConfig().getBoolean("Setting.Menu-AuthMe-Geyser")) {
            AuthMeApi.getInstance().forceRegister(player, plugin.getConfig().getString("Setting.Password-Login-Geyser"), true);
            player.sendMessage(color.transalate(plugin.getConfig().getString("Message.Auto-Register")));
        } else {
            sendRegisterMenu(player, null);
        }
    }

    private void sendLoginMenu(Player player, String label) {
        CustomForm.Builder formBuilder = createFormBuilder("Menu.Title-Login", "Menu.Title-Login-Password", "Menu.Input-Login-Placeholder");
        formBuilder.responseHandler((form, responseData) -> handleLoginFormResponse(player, form.parseResponse(responseData)));
        addLabelToForm(formBuilder, label);
        if (GeyserApi.api().isBedrockPlayer(player.getUniqueId())){
            GeyserApi.api().sendForm(player.getUniqueId(), formBuilder.build().newForm());
        }
    }

    private void handleLoginFormResponse(Player player, CustomFormResponse loginForm) {
        if (!loginForm.isCorrect()) {
            sendLoginMenu(player, plugin.getConfig().getString("Message.Not-Enough"));
            return;
        }

        String password = loginForm.getInput(0);
        if (AuthMeApi.getInstance().checkPassword(player.getName(), password)) {
            AuthMeApi.getInstance().forceLogin(player);
        } else {
            sendLoginMenu(player, plugin.getConfig().getString("Message.Wrong-Password"));
        }
    }

    private void sendRegisterMenu(Player player, String label) {
        CustomForm.Builder formBuilder = createFormBuilder("Menu.Title-Register", "Menu.Title-Register-Password", "Menu.Input-Register-Placeholder");
        formBuilder.input(
                color.transalate(plugin.getConfig().getString("Menu.Title-Register-RePassword")),
                color.transalate(plugin.getConfig().getString("Menu.Input-Register-RePlaceholder"))
        );
        formBuilder.responseHandler((form, responseData) -> handleRegisterFormResponse(player, form.parseResponse(responseData)));
        addLabelToForm(formBuilder, label);
        if (GeyserApi.api().isBedrockPlayer(player.getUniqueId())){
            GeyserApi.api().sendForm(player.getUniqueId(), formBuilder.build().newForm());
        }
    }

    private void handleRegisterFormResponse(Player player, CustomFormResponse registerForm) {
        if (!registerForm.isCorrect()) {
            sendRegisterMenu(player, plugin.getConfig().getString("Message.Not-Enough"));
            return;
        }

        String password = registerForm.getInput(0);
        String rePassword = registerForm.getInput(1);
        if (rePassword.equals(password)) {
            AuthMeApi.getInstance().forceRegister(player, password, true);
        } else {
            sendRegisterMenu(player, plugin.getConfig().getString("Message.Not-Same"));
        }
    }

    private CustomForm.Builder createFormBuilder(String titleConfigKey, String inputTitleConfigKey, String inputPlaceholderConfigKey) {
        return CustomForm.builder()
                .title(color.transalate(plugin.getConfig().getString(titleConfigKey)))
                .input(
                        color.transalate(plugin.getConfig().getString(inputTitleConfigKey)),
                        color.transalate(plugin.getConfig().getString(inputPlaceholderConfigKey))
                );
    }

    private void addLabelToForm(CustomForm.Builder formBuilder, String label) {
        if (label != null) {
            formBuilder.label(color.transalate(label));
        }
    }
}
