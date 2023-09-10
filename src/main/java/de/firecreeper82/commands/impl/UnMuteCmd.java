package de.firecreeper82.commands.impl;

import de.firecreeper82.Main;
import de.firecreeper82.commands.Command;
import de.firecreeper82.exceptions.exceptions.*;
import de.firecreeper82.logging.Logger;
import de.firecreeper82.permissions.Permission;
import de.firecreeper82.util.Util;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static de.firecreeper82.Main.*;

public class UnMuteCmd extends Command {


    public UnMuteCmd(String[] aliases, String description, List<String> requiredArgs, Permission requiredPerm) {
        super(aliases, description, requiredArgs, requiredPerm);

        Main.jda.updateCommands().addCommands(
                Commands.slash(aliases[0], description)
                        .addOption(OptionType.USER, "user", "The user to unmute", true)
        ).queue();
    }

    @Override
    public void onCommand(String[] args, Message message, Member member) throws MemberNotFoundException, MemberIsNotMutedException, RoleNoFoundException {
        Member muteMember = Util.getMemberFromString(args[0], message);

        if (muteMember == null)
            throw new MemberNotFoundException("The member you are trying to unmute could not be found.");

        Role muteRole = message.getGuild().getRoleById(getMutedRoleId());
        if (muteRole == null)
            throw new RoleNoFoundException("The muting role could not be found. Check the config to confirm the role id.");

        if(!muteMember.getRoles().contains(muteRole))
            throw new MemberIsNotMutedException("The member you are trying to unmute is not muted.");

        message.getGuild().removeRoleFromMember(muteMember, muteRole).queue();

        removeFromJsonFile(muteMember);

        sendConfirmEmbed(message, member, muteMember);

        EmbedBuilder eb = Util.createEmbed(
                "You were unmuted in " + message.getGuild().getName(),
                null,
                "You were unmuted in the server by " + member.getEffectiveName(),
                "Unmuted",
                Instant.now(),
                null,
                null
        );

        notifyUser(eb, muteMember.getUser());
    }

    @Override
    public void onSlashCommand(SlashCommandInteractionEvent event) throws MemberNotFoundException, RoleNoFoundException, MemberIsNotMutedException {
        Member muteMember = event.getOption("user", OptionMapping::getAsMember);

        if(event.getMember() == null)
            return;

        if (muteMember == null)
            throw new MemberNotFoundException("The member you are trying to unmute could not be found.");

        Role muteRole = event.getGuild().getRoleById(getMutedRoleId());
        if (muteRole == null)
            throw new RoleNoFoundException("The muting role could not be found. Check the config to confirm the role id.");

        if(!muteMember.getRoles().contains(muteRole))
            throw new MemberIsNotMutedException("The member you are trying to unmute is not muted.");

        event.getGuild().removeRoleFromMember(muteMember, muteRole).queue();

        removeFromJsonFile(muteMember);

        event.replyEmbeds(createConfirmEmbed(event.getMember(), muteMember).build()).setEphemeral(true).queue();

        EmbedBuilder eb = Util.createEmbed(
                "You were unmuted in " + event.getGuild().getName(),
                null,
                "You were unmuted in the server by " + event.getMember().getEffectiveName(),
                "Unmuted",
                Instant.now(),
                null,
                null
        );

        notifyUser(eb, muteMember.getUser());
    }

    @SafeVarargs
    @Override
    public final <T> void sendConfirmEmbed(Message message, Member member, T... additionalArgs) {
        Member muteMember = (Member) additionalArgs[0];
        EmbedBuilder eb = createConfirmEmbed(member, muteMember);

        message.getChannel().sendMessageEmbeds(eb.build()).queue(msg -> msg.delete().queueAfter(Main.getCommandFeedbackDeletionDelayInSeconds(), TimeUnit.SECONDS));
        if(Main.isLogCommandUsage()) {
            Logger.logCommandUsage(eb, this, member, message.getChannel());
        }
    }

    @NotNull
    private static EmbedBuilder createConfirmEmbed(Member member, Member muteMember) {
        EmbedBuilder eb = Util.createEmbed(
                "Unmuted " + muteMember.getEffectiveName(),
                Color.GREEN,
                "Successfully unmuted " + muteMember.getAsMention() + "!",
                "Unmuted by " + member.getEffectiveName(),
                Instant.now(),
                null,
                null
        );
        return eb;
    }

    @SuppressWarnings("unchecked")
    private static void removeFromJsonFile(Member muteMember) {
        for(Object object : readMutedMembers()) {
            JSONObject jsonObject = (JSONObject) object;

            if(!jsonObject.get("MemberID").equals(muteMember.getId()))
                continue;
            Guild guild = jda.getGuildById((String) jsonObject.get("GuildID"));
            if(guild == null)
                return;

            JSONArray objects = readMutedMembers();

            JSONArray writeObjects = new JSONArray();
            for(Object o : objects) {
                if(!(o instanceof JSONObject jObject))
                    continue;
                if(!jObject.get("MemberID").equals(muteMember.getId()))
                    writeObjects.add(jObject);

            }

            writeMutedMembersToJsonFile(writeObjects);
        }
    }
}
