import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.HTTPQuery;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;

public class ModuleQuestionParty extends Module {
	protected Command cmd;
	public Pattern headerPattern = Pattern.compile("<h1>(.+?)</h1>");
	public Pattern itemPattern = Pattern.compile("<li>(.+)");
	
	public String getQ() {
		HTTPQuery q = new HTTPQuery("http://questionparty.com/questions/rand/");
		
		q.connect(true,false);
		String html = q.readWhole();
		q.close();
		
		Matcher matcher = headerPattern.matcher(html);
		if(!matcher.find())
			return "";
		String question = matcher.group(1);
		
		matcher = itemPattern.matcher(html);
		ArrayList<String> answers = new ArrayList<String>();
		while (matcher.find()) {
			answers.add(matcher.group(1));
		}
		
		StringBuilder sb = new StringBuilder();
		int i = Math.min(answers.size(),5);
		Random rnd = new Random();
		while (i-- > 0) {
			if (sb.length() != 0) sb.append(" | ");
			int n = rnd.nextInt(answers.size());
			sb.append(answers.get(n).trim()); answers.remove(n);
		}
		sb.insert(0, '\n');
		sb.insert(0, question);
		return StringEscapeUtils.unescapeHtml4(sb.toString());
	}
	
	public String name() {return "questionparty";}
	public void onEnable() {
		Command.addCommands(this, cmd = new CmdQuestionParty());
		Command.addCommand(this, "qparty", cmd);
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public class CmdQuestionParty extends Command {
		public String command() {return "questionparty";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			StringBuilder sb = new StringBuilder();
			sb.append("questionparty/qparty");
			sb.append("\nquestionparty - random question with up to 5 random answers");
			return sb.toString();
		}
		
		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			callback.append(getQ());
		}
	}
}