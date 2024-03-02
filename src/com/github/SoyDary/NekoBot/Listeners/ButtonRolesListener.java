package com.github.SoyDary.NekoBot.Listeners;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.github.SoyDary.NekoBot.Main;
import com.google.common.collect.Lists;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Component.Type;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu.SelectTarget;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInput.Builder;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

public class ButtonRolesListener extends ListenerAdapter {

	Main main;
	public ButtonRolesListener(Main main) {
		this.main = main;
	}
	
    public void onMessageContextInteraction(MessageContextInteractionEvent e) {
    	if(e.getName().equals("Botones de rol")) {
    		if(!e.getTarget().getAuthor().getId().equals(e.getJDA().getSelfUser().getId())) {
    			e.reply("> :x: Solo se pueden editar mensajes propios.").setEphemeral(true).queue();
    			return;
    		}
    		MessageCreateBuilder builder = new MessageCreateBuilder();
    		Button bt = Button.secondary("BUTTON_ROLE:STYLE", Emoji.fromUnicode("👍"));
    		builder.addActionRow(bt);
    		builder.addActionRow(EntitySelectMenu.create("BUTTON_ROLE:ROLE="+UUID.randomUUID().toString(), SelectTarget.ROLE).setRequiredRange(1, 1).setPlaceholder("Selecciona un rol").build());
    		builder.addActionRow(
    				Button.of(ButtonStyle.SECONDARY, "BUTTON_ROLE:LABEL", "Texto", Emoji.fromUnicode("✏️")),
    				Button.of(ButtonStyle.SECONDARY, "BUTTON_ROLE:EMOJI", "Emoji", bt.getEmoji()),
    				Button.of(ButtonStyle.SUCCESS, "BUTTON_ROLE:CONFIRM="+e.getTarget().getId()+";0", "Crear", Emoji.fromUnicode("✅")).asDisabled());
    		e.reply(builder.build()).setEphemeral(true).queue();   		 		
    	}
    }
    
    public void onButtonInteraction(ButtonInteractionEvent e) {
    	String id = e.getButton().getId();
    	if(id.equals("BUTTON_ROLE:STYLE")) {
    		e.editButton(e.getButton().withStyle(checkStyle(e.getButton().getStyle()))).queue();
    		return;
    	}
    	if(id.equals("BUTTON_ROLE:EMOJI")) {
    		Builder content = TextInput.create("BUTTON_ROLE:EMOJI", "Contenido", TextInputStyle.SHORT).setRequired(false);
    		Emoji emoji = e.getMessage().getComponents().get(0).getButtons().get(0).getEmoji();
    		content.setValue(emoji != null ? emoji.getFormatted() : null);
    		content.setPlaceholder("Emoji unicode o en formato <:EMOJI:ID>");
    		e.replyModal(Modal.create("BUTTON_ROLE:EMOJI", "Editar emoji").addComponents(ActionRow.of(content.build())).build()).queue();
    		return;
    	}
    	if(id.equals("BUTTON_ROLE:LABEL")) {
    		Builder content = TextInput.create("BUTTON_ROLE:LABEL", "Contenido", TextInputStyle.SHORT).setRequired(false);
    		String label = e.getMessage().getComponents().get(0).getButtons().get(0).getLabel();
    		content.setValue(!label.isEmpty() ? label : null);
    		content.setPlaceholder("Texto del botón");
    		content.setMaxLength(80);
    		e.replyModal( Modal.create("BUTTON_ROLE:LABEL", "Editar contenido").addComponents(ActionRow.of(content.build())).build()).queue();
    		return;
    	}
    	if(id.startsWith("BUTTON_ROLE:CONFIRM=")) {	
    		String messageID = id.split("=")[1].split(";")[0];
    		String roleID = id.split(";")[1];
    		Message original = e.getChannel().retrieveMessageById(messageID).complete();
    		Button originalButton = original.getButtonById("GIVE_ROLE:"+roleID);   	
    		Button buttonRole = e.getMessage().getButtons().get(0).withId("GIVE_ROLE:"+roleID);
    		List<ActionRow> newRows = null;
    		List<ActionRow> rows = original.getActionRows();	
    		if(rows.isEmpty()) {
    			newRows = Lists.newArrayList();
    			newRows.add(ActionRow.of(buttonRole));
    			
    		} else {
     	       for (int i = 0; i < rows.size(); i++) {
   	            ActionRow row = rows.get(i);   
   	            if(row.getActionComponents().stream().allMatch(component -> component.getType() != Type.BUTTON) || row.getButtons().size() >= 5) continue;
   	            List<ItemComponent> components = row.getComponents();
   	            if(originalButton != null) {
   	            	for (int y = 0; y < components.size(); y++) {
   	            		if(!row.getButtons().get(y).getId().equals("GIVE_ROLE:"+roleID)) continue;
   	            		components.set(y, buttonRole);
   	            		break;
   	            	}
   	            } else {
   	            	components.add(buttonRole);
   	            }        
   	            newRows = new ArrayList<>(rows);
   	            newRows.set(i, ActionRow.of(components));
   	            break;
     	       }
    		} 
  	        if(newRows == null) {
  	        	if(rows.size() >= 5) {
  	        		e.reply("> Este mensaje no puede tener más botones.").setEphemeral(true).queue();
  	        		return;
  	        	} else {
  	   	            newRows = new ArrayList<>(rows);
  	   	            newRows.add(ActionRow.of(buttonRole));
  	        	}
  	        }
  	        List<ActionRow> editor = e.getMessage().getActionRows();
  	        editor.get(2).getComponents().set(2, e.getButton().withLabel("Editar"));
        	Button deleteButton = Button.danger("BUTTON_ROLE:DELETE", Emoji.fromUnicode("🗑️")).withLabel("Eliminar");
        	if(editor.get(2).getComponents().size() == 3) editor.get(2).getComponents().add(deleteButton);
        		else editor.get(2).getComponents().set(3, deleteButton);
  	        original.editMessageComponents(newRows).queue(r -> e.editComponents(editor).queue());
  	        return;
    	}   
    	if(id.startsWith("BUTTON_ROLE:DELETE")) {
    		String buttonID = e.getMessage().getActionRows().get(2).getActionComponents().get(2).getId();
    		String messageID = buttonID.split("=")[1].split(";")[0];
    		String roleID = buttonID.split(";")[1];		
    		Message original = e.getChannel().retrieveMessageById(messageID).complete();
    		List<ActionRow> newRows = Lists.newArrayList();
    		List<ActionRow> rows = original.getActionRows();
            for (int i = 0; i < rows.size(); i++) {
            	List<ItemComponent> components = Lists.newArrayList();
            	for(ActionComponent component : rows.get(i).getActionComponents()) {
            		if(!component.getId().equals("GIVE_ROLE:"+roleID)) components.add(component);
            	}
            	if(!components.isEmpty()) newRows.add(ActionRow.of(components));
            }
            

    		List<ActionRow> components = Lists.newArrayList();		
    		Button bt = Button.secondary("BUTTON_ROLE:STYLE", Emoji.fromUnicode("👍"));
    		components.add(ActionRow.of(bt));
    		components.add(ActionRow.of(EntitySelectMenu.create("BUTTON_ROLE:ROLE="+UUID.randomUUID().toString(), SelectTarget.ROLE).setRequiredRange(1, 1).setPlaceholder("Selecciona un rol").build()));
    		components.add(ActionRow.of(
    				Button.of(ButtonStyle.SECONDARY, "BUTTON_ROLE:LABEL", "Texto", Emoji.fromUnicode("✏️")),
    				Button.of(ButtonStyle.SECONDARY, "BUTTON_ROLE:EMOJI", "Emoji", bt.getEmoji()),
    				Button.of(ButtonStyle.SUCCESS, "BUTTON_ROLE:CONFIRM="+original.getId()+";0", "Crear", Emoji.fromUnicode("✅")).asDisabled()
    				));
    		
    		
            original.editMessageComponents(newRows).queue(r -> {
        		List<ActionRow> editor = Lists.newArrayList();		
        		editor.add(ActionRow.of(Button.secondary("BUTTON_ROLE:STYLE", Emoji.fromUnicode("👍"))));
        		editor.add(ActionRow.of(EntitySelectMenu.create("BUTTON_ROLE:ROLE="+UUID.randomUUID().toString(), SelectTarget.ROLE).setRequiredRange(1, 1).setPlaceholder("Selecciona un rol").build()));
        		editor.add(ActionRow.of(
        				Button.of(ButtonStyle.SECONDARY, "BUTTON_ROLE:LABEL", "Texto", Emoji.fromUnicode("✏️")),
        				Button.of(ButtonStyle.SECONDARY, "BUTTON_ROLE:EMOJI", "Emoji", bt.getEmoji()),
        				Button.of(ButtonStyle.SUCCESS, "BUTTON_ROLE:CONFIRM="+original.getId()+";0", "Crear", Emoji.fromUnicode("✅")).asDisabled()
        				));
        		e.editComponents(editor).queue();
            });
            return;
    	} 
    	if(id.startsWith("GIVE_ROLE:")) {
    		Role rol = e.getGuild().getRoleById(id.split(":")[1]);
    		if(rol == null) {
    			e.reply("> El rol asignado a este botón ya no existe.").setEphemeral(true).queue(r -> {
    	    		List<ActionRow> newRows = Lists.newArrayList();	    		
    	    		List<ActionRow> rows = e.getMessage().getActionRows();
    	            for (int i = 0; i < rows.size(); i++) {
    	            	List<ItemComponent> components = Lists.newArrayList();
    	            	for(ActionComponent component : rows.get(i).getActionComponents()) {
    	            		if(!component.getId().equals(id)) components.add(component);
    	            	}
    	            	if(!components.isEmpty()) newRows.add(ActionRow.of(components));
    	            }
    	            e.getMessage().editMessageComponents(newRows).queue();
    	            
    			});
    			return;
    		} 
    		if(!e.getMember().getRoles().contains(rol)) {
    			e.getGuild().addRoleToMember(e.getMember(), rol).reason("Botón de rol").queue(
    					success -> e.reply("> Recibiste el rol "+rol.getAsMention()).setAllowedMentions(Collections.emptySet()).setEphemeral(true).queue(), 
    					error -> e.reply("> Hubo un error al intentar otorgarte el rol "+rol.getAsMention()).setAllowedMentions(Collections.emptyList()).setEphemeral(true).queue());
    		} else {
    			e.deferEdit().queue();
    		}
    		return;
    	}
    }
    
    public void onModalInteraction(ModalInteractionEvent e) {
    	String id = e.getModalId();
    	if(id.equals("BUTTON_ROLE:EMOJI")) { 
    		String value = e.getValue("BUTTON_ROLE:EMOJI").getAsString();
    		List<ActionRow> rows = e.getMessage().getActionRows();
    		if(value.isEmpty()) {      
    			e.deferEdit().queue();
                return;
    		} else {
        		Emoji emoji = findEmoji(value);
                rows.get(0).getComponents().set(0, rows.get(0).getButtons().get(0).withEmoji(emoji));
                rows.get(2).getComponents().set(1, rows.get(2).getButtons().get(1).withEmoji(emoji));
                Message original = e.getMessageChannel().retrieveMessageById(rows.get(2).getButtons().get(2).getId().split("=")[1].split(";")[0]).complete();
                original.addReaction(emoji).queue(x -> e.editComponents(rows)
            			.queue(s -> original.removeReaction(emoji, e.getJDA().getSelfUser())
            					.queue()), y -> e.reply("> :x: Emoji inválido").setEphemeral(true).queue());           
    		}
    		return;
    	}
    	if(id.equals("BUTTON_ROLE:LABEL")) {
    		String value = e.getValue("BUTTON_ROLE:LABEL").getAsString();
    		List<ActionRow> rows = e.getMessage().getActionRows();
    		if(value.isEmpty()) { 
    			Button button = rows.get(0).getButtons().get(0);	
    			rows.get(0).getComponents().set(0, Button.of(button.getStyle(), button.getId(), button.getEmoji()));
    			e.editComponents(rows).queue();
                return;
    		} else {
    			rows.get(0).getComponents().set(0, rows.get(0).getButtons().get(0).withLabel(value));
    			e.editComponents(rows).queue();
    		}
    		return;
    	}	
    }

    public void onEntitySelectInteraction(EntitySelectInteractionEvent e) {
        if (e.getComponentId().startsWith("BUTTON_ROLE:ROLE")) {
        	if(e.getMentions().getRoles().isEmpty()) return;
        	Role role = e.getMentions().getRoles().get(0);
       		Role ownRole = null;
            for(Role r : e.getGuild().getMember(e.getJDA().getSelfUser()).getRoles()) {
            	if(ownRole == null) ownRole = r;
            	if(r.getPosition() > ownRole.getPosition()) ownRole = r;
            }
            if(role.isManaged() || ownRole == null || !ownRole.canInteract(role)) {
            	e.reply("> Este rol no puede ser seleccionado porque el bot no tiene permisos para entregarlo.").setEphemeral(true).queue();
            	return;
            }
            List<ActionRow> rows = e.getMessage().getActionRows();        
            Button confirmButton = rows.get(2).getButtons().get(2);
            String messageID = confirmButton.getId().split("=")[1].split(";")[0];
            String roleID = role.getId();                
            Message message = e.getChannel().retrieveMessageById(messageID).complete();
            rows.get(2).getComponents().set(2, confirmButton.withId("BUTTON_ROLE:CONFIRM="+messageID+";"+roleID).asEnabled());
            Button currentButton = message.getButtonById("GIVE_ROLE:"+role.getId());     
            if(currentButton != null) {
            	rows.get(2).getComponents().set(2, confirmButton.withId("BUTTON_ROLE:CONFIRM="+messageID+";"+roleID).withLabel("Editar").asEnabled());
            	rows.get(2).getComponents().set(1, rows.get(2).getButtons().get(1).withEmoji(currentButton.getEmoji()));
            	rows.get(0).getComponents().set(0, currentButton);
            	Button deleteButton = Button.danger("BUTTON_ROLE:DELETE", Emoji.fromUnicode("🗑️")).withLabel("Eliminar");
            	if(rows.get(2).getComponents().size() == 3) rows.get(2).getComponents().add(deleteButton);
            		else rows.get(2).getComponents().set(3, deleteButton);
            } else {
            	rows.get(2).getComponents().set(2, confirmButton.withId("BUTTON_ROLE:CONFIRM="+messageID+";"+roleID).withLabel("Crear").asEnabled());
            	if(rows.get(2).getComponents().size() == 4) rows.get(2).getComponents().remove(3);
            }
            e.editComponents(rows).queue();        
        }
    }
    
    private Emoji findEmoji(String stringEmoji) {
    	Emoji emoji = null;
    	if(stringEmoji.length() == 1) {
    		return Emoji.fromUnicode(stringEmoji);
    	}
    	List<RichCustomEmoji> emojis = main.getJda().getEmojisByName(stringEmoji, true);
    	if(!emojis.isEmpty()) return emojis.get(0);
    	String name = StringUtils.substringBetween(stringEmoji, ":", ":");
    	if(name != null) {
        	List<RichCustomEmoji> emos = main.getJda().getEmojisByName(name, true);
        	if(!emos.isEmpty()) return emos.get(0); 
    	}
    	try {
    		emoji =  Emoji.fromFormatted(stringEmoji);
    		return emoji;
    	} catch(Exception e) {}	
    	return emoji;
    }
    
    private ButtonStyle checkStyle(ButtonStyle style) { 
    	if(style == ButtonStyle.SECONDARY) return ButtonStyle.PRIMARY;
    	if(style == ButtonStyle.PRIMARY) return ButtonStyle.SUCCESS;
    	if(style == ButtonStyle.SUCCESS) return ButtonStyle.DANGER;
    	if(style == ButtonStyle.DANGER) return ButtonStyle.SECONDARY;
    	return style;
    }    
}
