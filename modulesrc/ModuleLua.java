import java.io.ByteArrayInputStream;
import java.io.FilePermission;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.security.Permission;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.*;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

import pl.shockah.StringTools;
import pl.shockah.shocky.ScriptModule;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Command.EType;

public class ModuleLua extends ScriptModule {
	protected Command cmd;
	protected SecurityManager secure = new LuaSecurityManager();
	LuaTable env = null;

	@Override
	public String name() {return "lua";}
	@Override
	public String identifier() {return "lua";}
	@Override
	public void onEnable() {
		Command.addCommands(cmd = new CmdLua());
		env = new LuaTable();
		env.load(new JseBaseLib());
		env.load(new PackageLib());
		env.load(new TableLib());
		env.load(new StringLib());
		env.load(new JseMathLib());
		LuaThread.setGlobals(env);
	}
	@Override
	public void onDisable() {
		Command.removeCommands(cmd);
	}

	@Override
	public String parse(PircBotX bot, EType type, Channel channel, User sender, String code, String message) {
		if (code == null) return "";
		
		if (message != null) {
			String[] args = message.split(" ");
			String argsImp = StringTools.implode(args,1," "); if (argsImp == null) argsImp = "";
			env.set("argc",(args.length-1));
			env.set("args",argsImp.replace("\"","\\\""));
			env.set("ioru",(args.length-1 == 0 ? sender.getNick() : argsImp).replace("\"","\\\""));
			//env.set("arg",CoerceJavaToLua.coerce(Arrays.copyOfRange(args, 1, args.length)));
			LuaTable arg = new LuaTable();
			for (int i = 1; i < args.length; i++)
				arg.set(i, args[i]);
			env.set("arg",arg);
		}
		
		env.set("channel", channel.getName());
		env.set("bot", bot.getNick());
		env.set("sender", sender.getNick());
		
		Sandbox sandbox = new Sandbox(channel.getUsers().toArray(new User[0]));
		env.set("bot", CoerceJavaToLua.coerce(sandbox));

		LuaRunner r = new LuaRunner(code);

		SecurityManager sysSecure = System.getSecurityManager();
		System.setSecurityManager(secure);
		String output = null;
		final ExecutorService service = Executors.newFixedThreadPool(1);
		try {
		    Future<String> f = service.submit(r);
		    output = f.get(30, TimeUnit.SECONDS);
		}
		catch(TimeoutException e) {
		    output = "Script timed out";
		}
		catch(Exception e) {
		    throw new RuntimeException(e);
		}
		finally {
		    service.shutdown();
		    System.setSecurityManager(sysSecure);
		}
		if (output == null || output.isEmpty())
			return null;
		
		StringBuilder sb = new StringBuilder();
		for(String line : output.split("[\r\n]+")) {
			if (sb.length() != 0) sb.append(" | ");
			sb.append(line);
		}

		return StringTools.limitLength(sb);
	}
	
	public class CmdLua extends Command {
		public String command() {return "lua";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			return "lua\nlua {code} - runs Lua code";
		}
		public boolean matches(PircBotX bot, EType type, String cmd) {
			return cmd.equals(command());
		}

		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			String[] args = message.split(" ");
			if (args.length < 2) {
				callback.type = EType.Notice;
				callback.append(help(bot,type,channel,sender));
				return;
			}

			System.out.println(message);
			String output = parse(bot,type,channel,sender,StringTools.implode(args,1," "),null);
			if (output != null && !output.isEmpty())
				callback.append(output);
		}
	}
	
	public class Sandbox {
		private Random rnd = new Random();
		private final User[] users;
		
		public Sandbox(User[] users) {
			this.users = users;
		}
		
		public String randnick() {
			return users[rnd.nextInt(users.length)].getNick();
		}
		
		public String munge(String in) {
			return Utils.mungeNick(in);
		}
		
		public String odd(String in) {
			return Utils.odd(in);
		}
		
		public String flip(String in) {
			return Utils.flip(in);
		}
		
		public String reverse(String in) {
			return new StringBuilder(in).reverse().toString();
		}
		
		public String toString() {
			return "Yes it is a bot";
		}
	}
	
	private static class LuaSecurityManager extends SecurityManager 
    {
    	@Override
    	public void checkPermission(Permission perm) 
    	{
    		if (perm instanceof FilePermission)
    			throw new SecurityException();
    	}
    	@Override
    	public void checkPermission(Permission perm, Object context) 
    	{
    		if (perm instanceof FilePermission)
    			throw new SecurityException();
    	}
    	@Override
    	public void checkExit(int status) {
    		throw new SecurityException();
    	}
    }
	
	public class LuaRunner implements Callable<String> {
		
		private final String code;
		
		public LuaRunner(String c) {
			code = c;
		}

		@Override
		public String call() throws Exception {
			ByteOutputStream sw = new ByteOutputStream();
			PrintStream pw = new PrintStream(sw);
			BaseLib.instance.STDERR = pw;
			BaseLib.instance.STDOUT = pw;
			
			try {
				LuaFunction func = LuaC.instance.load(new ByteArrayInputStream(code.getBytes()), "script", env);
				Object out = func.invoke();
				if (sw.size() != 0)
					return new String(sw.getBytes(),0,sw.size(),Charset.forName("UTF-8"));
				if (out != null)
					return out.toString();
			}
			catch(LuaError ex) {
				return ex.getMessage();
			}
			return null;
		}
		
	}
}