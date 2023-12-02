package com.github.SoyDary.NekoBot.Objects;

import java.util.concurrent.TimeUnit;

import com.github.SoyDary.NekoBot.Launcher;
import com.github.SoyDary.NekoBot.Main;
import com.github.SoyDary.NekoBot.Listeners.ButtonRolesListener;
import com.github.SoyDary.NekoBot.Listeners.JDAListener;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class Bot extends Thread {
	private JDA jda;
	Main main;
	JDAListener listener;
	ButtonRolesListener buttonRolesListener;
	
	public Bot(Main main) {
		super("JDA Main Thread");
		this.start();
		this.main = main;
	}
	
	public void registerEvents() {
		jda.addEventListener(listener = new JDAListener(main));
		jda.addEventListener(buttonRolesListener = new ButtonRolesListener(main));
	}

	public boolean initBot() {
	    stopBot();
	    try {
	    	
	    	this.jda = JDABuilder.createLight(Launcher.bottoken)
	    			.enableCache(CacheFlag.EMOJI)
	    			.setMemberCachePolicy(MemberCachePolicy.ALL)
	    			.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_EMOJIS_AND_STICKERS, GatewayIntent.GUILD_MEMBERS,  GatewayIntent.GUILD_MESSAGE_REACTIONS)
	    			.build();
	    } catch (Exception ex) {
	    	ex.printStackTrace();
	    	return false;
	    }	  
	   return true;
	}
	
	  public JDA getJda() {
		  return this.jda;
	  }
	  
	  public void sendMessage(TextChannel channel, String message) {
		  sendMessage(channel, message, -1);
	  }
	  
	  public void sendMessage(TextChannel channel, String message, int duration) {
		  channel.sendMessage(message).queue(msg -> {
			  if(duration != -1) {
				  msg.delete().queueAfter(duration, TimeUnit.SECONDS);
				}
			});
	  }
	  
	  public void stopBot() {
		  if (jda != null) {
			  jda.shutdownNow();
		    	try {
					jda.awaitShutdown(5, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		  }
		  if(listener != null) jda.removeEventListener(listener);
		  if(buttonRolesListener != null) jda.removeEventListener(buttonRolesListener);
	  }  
	  public String getInviteLink() {
		  return this.jda.getInviteUrl(new Permission[] {Permission.ADMINISTRATOR, Permission.USE_APPLICATION_COMMANDS});
	  }
}