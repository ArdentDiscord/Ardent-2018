package tk.ardentbot.commands.administration;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import tk.ardentbot.core.executor.Command;
import tk.ardentbot.core.misc.logging.BotException;
import tk.ardentbot.utils.discord.UserUtils;

import java.util.List;

public class Ban extends Command {
    public Ban(CommandSettings commandSettings) {
        super(commandSettings);
    }

    @Override
    public void noArgs(Guild guild, MessageChannel channel, User user, Message message, String[] args) throws Exception {
        if (args.length == 1) {
            sendTranslatedMessage("Ban users by typing /ban and then mentioning one or more users. The bot must have ban permissions",
                    channel, user);
        }
        else {
            Member userMember = guild.getMember(user);
            if (userMember.hasPermission(Permission.BAN_MEMBERS)) {
                List<User> mentionedUsers = message.getMentionedUsers();
                if (mentionedUsers.size() == 0) {
                    sendTranslatedMessage("You need to mention one or more users", channel, user);
                }
                else {
                    for (User mentioned : mentionedUsers) {
                        if (!guild.getMember(mentioned).hasPermission(userMember.getPermissions((Channel) channel))) {
                            guild.getController().ban(mentioned, 1).queue(aVoid -> {
                                try {
                                    sendTranslatedMessage("Successfully banned " + UserUtils.getNameWithDiscriminator(mentioned.getId()),
                                            channel, user);
                                }
                                catch (Exception e) {
                                    new BotException(e);
                                }
                            }, throwable -> {
                                try {
                                    sendTranslatedMessage("Failed to " + UserUtils.getNameWithDiscriminator(mentioned.getId()), channel,
                                            user);
                                }
                                catch (Exception e) {
                                    new BotException(e);
                                }
                            });
                        }
                        else sendTranslatedMessage("I cannot ban this user", channel, user);
                    }
                }
            }
            else sendTranslatedMessage("I need permission to ban users!", channel, user);
        }
    }

    @Override
    public void setupSubcommands() {
    }
}
