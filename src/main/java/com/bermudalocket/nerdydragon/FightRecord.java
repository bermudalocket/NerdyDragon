package com.bermudalocket.nerdydragon;

import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

public class FightRecord implements Serializable {

    private UUID associatedFightUUID;

    private double dragonMaxHealth;

    private long rawDuration;

    private String formattedDuration;

    private HashSet<Participant> participants = new HashSet<>();

    public FightRecord(EnderDragonFight fight) {
        this.associatedFightUUID = fight.getUUID();
        this.dragonMaxHealth = fight.getDragon().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(); // TODO NPE
    }

    public TextComponent getDescriptor() {
        String truncatedUUID = String.valueOf(associatedFightUUID.getMostSignificantBits());
        TextComponent text = new TextComponent(String.format(
            "%s... in %s",
            truncatedUUID, formattedDuration
        ));
        text.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(
            this.getIndividualMentions(System.lineSeparator())
        ).create()));
        return text;
    }

    public UUID getAssociatedFightUUID() {
        return this.associatedFightUUID;
    }

    public long getRawDuration() {
        return this.rawDuration;
    }

    public void setDuration(long durationInMS) {
        this.rawDuration = durationInMS;
        this.formattedDuration = DurationFormatUtils.formatDuration(durationInMS, Util.getHMSFormat(durationInMS));
    }

    public String getIndividualMentions(String separator) {
        ArrayList<String> individualMentions = new ArrayList<>();
        for (Participant participant : this.participants) {
            individualMentions.add(String.format(
                    "%s%s %s(%.2f)",
                    ChatColor.LIGHT_PURPLE, participant.getName(), ChatColor.GRAY, this.getRatio(participant)
            ));
        }
        return String.join(separator, individualMentions);
    }

    public String getWinMessage() {
        StringBuilder builder = new StringBuilder();
        builder.append("The dragon has been slain! The valiant ")
                .append(participants.size() == 1 ? "warrior " : "warriors ")
                .append(this.getIndividualMentions(", "))
                .append(String.format(
                    "prevailed in %s%s",
                    ChatColor.DARK_PURPLE, this.formattedDuration
                )
        );
        return builder.toString();
    }

    public void addParticipant(Player player) {
        this.participants.add(new Participant(player));
    }

    public HashSet<Participant> getParticipants() {
        return new HashSet<>(this.participants);
    }

    public Participant getParticipant(Player player) {
        for (Participant participant : this.participants) {
            if (participant.getUUID().equals(player.getUniqueId())) {
                return participant;
            }
        }
        return new Participant(player);
    }

    public double getRatio(Participant participant) {
        return participant.getDamage() / this.dragonMaxHealth;
    }

}
