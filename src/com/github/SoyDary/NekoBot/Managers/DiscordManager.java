package com.github.SoyDary.NekoBot.Managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.SoyDary.NekoBot.Main;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.Command.Type;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.ErrorResponse;

public class DiscordManager {
	
	Main main;
	Map<String, CommandData> commandData = new HashMap<String, CommandData>();
	
	public DiscordManager(Main main) {
		this.main = main;
		loadCommands();
	}
	
	public void loadCommands() {
		//unregisterCommands();
		main.getLogger().info("Verificando comandos...");
		commandData.put("embed", Commands.slash("embed", "Crea un embed")
				.addOption(OptionType.STRING, "json", "Embed a partir de un json")
				.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)));
		
		
		commandData.put("buttonrole", Commands.slash("buttonrole", "Crear y eliminar botones para otorgar roles").addSubcommands(
				new SubcommandData("create", "Craa un botón que otorga un rol al presionarse.")             
				.addOptions(	
						new OptionData(OptionType.ROLE, "rol", "Rol que otorga el botón al presionarse.").setRequired(true),
						new OptionData(OptionType.STRING, "color", "Color del botón")
							.addChoice("Azul", "PRIMARY")
							.addChoice("Gris", "SECONDARY")
							.addChoice("Verde", "SUCCESS")
							.addChoice("Rojo", "DANGER"),
						new OptionData(OptionType.STRING, "emoji", "Emoji del botón.").setMinLength(1),
						new OptionData(OptionType.STRING, "texto", "Texto del botón."),						
						new OptionData(OptionType.BOOLEAN, "nofificar", "Responder con un mensaje al presionarse.")), 
				
				new SubcommandData("delete", "Elimina botones de rol de un mensaje.")
				.addOptions(
						new OptionData(OptionType.CHANNEL, "canal", "Canal del mensaje").setRequired(true),
						new OptionData(OptionType.INTEGER, "id", "Canal del mensaje").setRequiredRange(20, 20).setRequired(true)))
				.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)));
		
		commandData.put("Editar", Commands.message("Editar")
				.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)));
		
		commandData.put("Copiar embed", Commands.message("Copiar embed")
				.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)));

		List<String> commands = new ArrayList<String>();
		for(Command cmd : main.getJda().retrieveCommands().complete()) {
			commands.add(cmd.getName());
			CommandData data = commandData.get(cmd.getName());
			if(cmd != null) updateCommand(data, cmd);
		}
		commandData.values().forEach(cmd -> {
			if(!commands.contains(cmd.getName())) registerCommand(cmd);
		});
	}
	
	public void unregisterCommands() {
		for (Command cmd : main.getJda().retrieveCommands().complete()) {
			if(commandData.keySet().contains(cmd.getName())) 
				cmd.delete().queue(c -> {
					main.getLogger().info("[----] Eliminado el comando "+cmd.getName());
				});
		}
	}
	
	
	public void updateCommand(CommandData commandData, Command command) {
		if(commandData.getType() == Type.SLASH) {
			updateSlashCommand((SlashCommandData )commandData, command);
			return;
		}
	}
	
	void registerCommand(CommandData command) {
		try {
			main.getJda().upsertCommand(command).queue(cmd ->
			{
				main.getLogger().info("[^^^^] Registrado el comando "+command.getName());
			}, new ErrorHandler().handle(ErrorResponse.APPLICATION_COMMAND_NAME_ALREADY_EXISTS, (e)-> {
				main.getLogger().warning("El comando '"+command.getName()+"' no pudo ser registrado porque ya hay otro con ese nombre.");
			}));
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}
	
	public boolean updateSlashCommand(SlashCommandData commandData, Command command) {
		List<OptionData> options = commandData.getOptions();
		DefaultMemberPermissions permissions = commandData.getDefaultPermissions();
		String label = commandData.getName();
		String description = commandData.getDescription();
		List<SubcommandData> oldSubCommands = command.getSubcommands().stream().map(SubcommandData::fromSubcommand).collect(Collectors.toList());
		List<SubcommandData> subCommands = commandData.getSubcommands();
		boolean updateSubCommands = false;
		if(oldSubCommands.size() == subCommands.size()) {
			for (int i = 0; i < oldSubCommands.size(); i++) {	
				String osc = oldSubCommands.get(i).toData().toString();
				String sc = subCommands.get(i).toData().toString();
				if(osc.equals(sc)) continue;
				updateSubCommands = true;
				break;
			}
		}
		
		
		try {
			
			List<OptionData> new_options = commandData.getOptions();
			List<OptionData> old_options = new ArrayList<OptionData>();
			old_options.addAll(command.getOptions().stream().map(OptionData::fromOption).collect(Collectors.toList()));
			
			String n_options = new_options.stream()
				    .map(OptionData::toData)
				    .map(Object::toString)
				    .collect(Collectors.joining());
		    String o_options = old_options.stream()
				    .map(OptionData::toData)
				    .map(Object::toString)
				    .collect(Collectors.joining());
		    
			if(!n_options.toString().equals(o_options.toString())) 
				command.editCommand().addOptions(options).queue(s -> {
					main.getLogger().info("[>>>>] Actualizadas las opciones del comando '"+s.getName()+"'");
				});
			
			long new_perms = 0; try {new_perms = commandData.getDefaultPermissions().getPermissionsRaw();} catch(Exception e) {}
			long old_perms = 0; try {old_perms = command.getDefaultPermissions().getPermissionsRaw();} catch(Exception e) {}		
			if(new_perms != old_perms) {
				command.editCommand().setDefaultPermissions(permissions).queue(s -> {
					main.getLogger().info("[>>>>] Actualizados los permisos del comando '"+s.getName()+"'");
				});
			} 
			
	    	if(!command.getName().equals(label))
	    		command.editCommand().setName(label).queue(s -> {
	    			main.getLogger().info("[>>>>] Actualizado el label del comando '"+s.getName()+"'");
	    		});	 
	    	
	    	if(!command.getDescription().equals(description)) 
	    		command.editCommand().setDescription(description).queue(s -> {
	    			main.getLogger().info("[>>>>] Actualizada la descripcion del comando '"+s.getName()+"'");
	    		});
	    	
			if(updateSubCommands)
				command.editCommand().addSubcommands(subCommands).queue(s -> {
					main.getLogger().info("[>>>>] Actualizados los sub comandos del comando '"+s.getName()+"'");
				});
	    	return true;
		} catch (Exception e) {
			main.getLogger().severe("Hubo un error al actualizar los datos comando "+command.getName()+"  >  "+e.getLocalizedMessage());
			return false;
		}
	}
	

}