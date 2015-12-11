package codebots.bots;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import codebots.bot.ReadonlyBot;
import codebots.controller.Globals;
import codebots.gameobjects.*;
import codebots.gameobjects.AddressBook.AddressType;
public class SwarmBot extends DefaultCodeBot {
	private final String TEAM = "Swarmer";
	private final List<FunctionType> funcList;
	{
		List<FunctionType> list = new ArrayList<FunctionType>();
		list.add(FunctionType.SELECT_FUNCTION_TO_BLOCK);
		list.add(FunctionType.GET_FLAG);
		list.add(FunctionType.SELECT_MESSAGE_RECIPIENTS);
		list.add(FunctionType.SEND_MESSAGE);
		list.add(FunctionType.READ_DATA);
		list.add(FunctionType.SELECT_FUNCTION_TO_REPLACE);
		list.add(FunctionType.SELECT_ATTACK_TARGET);
		list.add(FunctionType.PROCESS_MESSAGE);
		funcList = Collections.unmodifiableList(list);
	}
	private final List<String> varList;
	{
		List<String> list = new ArrayList<String>();
		list.add("SwarmTarget");
		list.add("FuncToReplace");
		list.add("MessageType");
		varList = Collections.unmodifiableList(list);
	}
	private static int atkCount = 0;
	private static int neuCount = 0;
	private static int truCount = 0;
	
    public IPAddress selectMessageRecipient() {
    	if(getTurnNumber() == 0) {
			getAddressBook().add(personalAddress(),AddressBook.AddressType.TRUSTED);
		}
		cleanVars();
		AddressBook book = getAddressBook();
		//reply to any bots we've identified as friendly
		//TRUSTED bots don't need to be messaged
		List<IPAddress> l = book.getAddressesOfType(AddressBook.AddressType.UNTRUSTED);
		if(l.size() > 0) {
			getVariables().add("MessageType","UNTRUSTED");
			return l.get(getRandom().nextInt(l.size()));
		}
		//if no potential friendlies, message a random neutral bot
		l = book.getAddressesOfType(AddressBook.AddressType.NEUTRAL);
		if(l.size() > 0) {
			getVariables().add("MessageType","NEUTRAL");
			return l.get(getRandom().nextInt(l.size()));
		}
		//if no neutrals, message a random bot
		getVariables().add("MessageType","RANDOM");
	    return book.getAddress(getRandom().nextInt(book.size()));
    }
    
    public Message sendMessage() {
		cleanVars();
		if(getVariables().has("MessageType")) {
			if(getVariables().get("MessageType").equals("RANDOM")) {
				//messaging an arbitrary bot and tell it to attack a target
				//target chosen uses this class's code, not the *bot's*
				//current selectAttackTarget()
				return new Message(Message.MessageType.HELP,this.selectAttackTarget());
			}
		}
		//this is cheating: IPAddress takes a string parameter and makes no attempt
		//to insure that that string is a valid IP address.  Only my processMessage
		//will know how to handle this which prevents other bots spoofing me and
		//gives other bots useless information.  Allows for 2-way confirmation.
        return new Message(Message.MessageType.STOP,personalAddress());
    }
    
    @Override
    public void processMessage(IPAddress source, Message message) {
		cleanVars();
		AddressBook book = getAddressBook();
		if(message.getType() == Message.MessageType.STOP) {
			//if from ourselves...
			if(message.getAddress() != null && message.getAddress().toString().equals(source.toString())) {
				AddressType ty = book.getAddressType(source);
				//If not already trusted or semi-trusted
				if(ty != AddressBook.AddressType.TRUSTED && ty != AddressBook.AddressType.TO_DEFEND) {
					book.remove(source);
					//add as possible allies to verify
					book.add(source,AddressBook.AddressType.UNTRUSTED);
				}
			}
			else {
				//this message isn't from me an actual friendly, must be an enemy trying to spoof!
				if(book.getAddressType(source) != null)
					book.add(source,AddressBook.AddressType.TO_ATTACK);
				if(book.getAddressType(message.getAddress()) != null)
					book.add(message.getAddress(),AddressBook.AddressType.TO_ATTACK);
			}
		}
		else {
			//this address sent me a message, categorize them as neutral
			//also categorize the IP they sent along as neutral
			//assuming we know nothing
			if(book.getAddressType(source) != null)
				book.add(source,AddressBook.AddressType.NEUTRAL);
			if(book.getAddressType(message.getAddress()) != null)
				book.add(message.getAddress(),AddressBook.AddressType.NEUTRAL);
		}
    }
    
    public IPAddress selectAttackTarget() {
		cleanVars();
		AddressBook book = getAddressBook();
		List<IPAddress> l;
		//if only 5% of the game remains, prioritize spreading the flag
		if(getTurnNumber() >= Globals.NUM_TURNS_IN_ROUND * 0.95f) {
			l = book.getAddressesOfType(AddressBook.AddressType.TO_ATTACK);
			if(l.size() > 0) {
				getVariables().add("SwarmTarget",l.get(0).toString());
				return l.get(0);
			}
		}
		
		//target last target first
		//then target test untrusted (to see if they are allies)
    	//then target corrupted allies
		//then target enemies
		//then target neutrals
		//then target allies (to insure they are still allies)
		//finally target random address
		
		//skip last target during the first 5% of the game, we'd rather verify allies
		//and collect more addresses
		if(getTurnNumber() <= Globals.NUM_TURNS_IN_ROUND * 0.05f) {
			if(getVariables().has("SwarmTarget")) {
				String target = getVariables().get("SwarmTarget");
				if(getVariables().has(target)) {
					int v = getV();
					if(v < 8 && v >= 0) {
						return new IPAddress(target);
					}
				}
			}
		}
		IPAddress rr;
		l = book.getAddressesOfType(AddressBook.AddressType.UNTRUSTED);
		if(l.size() > 0) {
			getVariables().add("SwarmTarget",l.get(0).toString());
			return l.get(0);
		}
		l = book.getAddressesOfType(AddressBook.AddressType.TO_DEFEND);
		if(l.size() > 0) {
			getVariables().add("SwarmTarget",l.get(0).toString());
			return l.get(0);
		}
		if(getRandom().nextBoolean()) {
			l = book.getAddressesOfType(AddressBook.AddressType.TO_ATTACK);
			if(l.size() > 0) {
				atkCount++;
				rr = book.getAddress(getRandom().nextInt(book.size()));
				getVariables().add("SwarmTarget",rr.toString());
		        return rr;
				//return l.get(0);
			}
		}
		if(getRandom().nextBoolean()) {
			l = book.getAddressesOfType(AddressBook.AddressType.TRUSTED);
			if(l.size() > 0) {
				truCount++;
				getVariables().add("SwarmTarget",l.get(0).toString());
				return l.get(0);
			}
		}
		l = book.getAddressesOfType(AddressBook.AddressType.NEUTRAL);
		if(l.size() > 0) {
			neuCount++;
			getVariables().add("SwarmTarget",l.get(0).toString());
			return l.get(0);
		}
		
		rr = book.getAddress(getRandom().nextInt(book.size()));
		getVariables().add("SwarmTarget",rr.toString());
        return rr;
    }

	@Override
    public void readData(ReadonlyBot bot) {
    	cleanVars();
    	
    	AddressBook book = getAddressBook();
    	Variables vars = getVariables();
    	IPAddress target = null;
    	String trg = vars.get("SwarmTarget");
    	
    	if(trg != null) {
	    	for(IPAddress n : book.allAddresses()) {
	    		if(n != null && trg.equals(n.toString())) {
	    			target = n;
	    			break;
	    		}
	    	}
    	}
    	if(target != null) {
			boolean fb = this.functionsMatch(bot,FunctionType.SELECT_FUNCTION_TO_BLOCK);
			boolean mr = this.functionsMatch(bot,FunctionType.SELECT_MESSAGE_RECIPIENTS);
			boolean gf = this.functionsMatch(bot,FunctionType.GET_FLAG);
			boolean sm = this.functionsMatch(bot,FunctionType.SEND_MESSAGE);
			boolean rd = this.functionsMatch(bot,FunctionType.READ_DATA);
			boolean fr = this.functionsMatch(bot,FunctionType.SELECT_FUNCTION_TO_REPLACE);
			boolean at = this.functionsMatch(bot,FunctionType.SELECT_ATTACK_TARGET);
			boolean pm = this.functionsMatch(bot,FunctionType.PROCESS_MESSAGE);
			
			int f1 = 0;
			//these four make up the bulk of a friendly bot
			f1 += fb?1:0;
			f1 += gf?1:0;
			f1 += rd?1:0;
			f1 += fr?1:0;
			//largely speaking, they're friendly
			if(f1 > 3) {
				book.add(target, AddressBook.AddressType.TRUSTED);
				if(pm) {
					//they're friendly, don't attack them next turn
					getVariables().remove("AttackTarget");
					//replace semi-random method
					getVariables().add("FuncToReplace",""+10);
				}
				//not fully converted yet AFAICT
				//if all methods match, skip
				else if(!(mr && sm && at)) {
					int v = getV()+1;
					getVariables().add("FuncToReplace",""+v);
				}
				else {
					//ever method matches, remove for next turn
					getVariables().remove("AttackTarget");
					//doesn't matter this turn what we do
					getVariables().add("FuncToReplace",""+(getTurnNumber()%8));
				}
			}
			else {
	            book.add(target, AddressBook.AddressType.TO_ATTACK);
	            int v = getV()+1;
				getVariables().add("FuncToReplace",""+v);
			}
    	}
    }
    
    public FunctionType selectFunctionToReplace() {
		cleanVars();
		//if only 5% of the game remains, prioritize spreading the flag
		if(getTurnNumber() >= Globals.NUM_TURNS_IN_ROUND * 0.95f) {
			return FunctionType.GET_FLAG;
		}
		
		//readData() determines what method we want to replace, given the target
		if(getVariables().has("FuncToReplace")) {
			int v = getV();
			if(v != clamp(v,0,7)) {
				//if the stored value is not valid
				v = 2;
			}
			return funcList.get(v);
		}
		//by default, replace selectFunctionToBlock()
        return FunctionType.SELECT_FUNCTION_TO_BLOCK;
    }

	public FunctionType selectFunctionToBlock() {
    	cleanVars();
    	if(this.getTurnNumber() <= 1) {
            return FunctionType.GET_FLAG;
    	}
        return FunctionType.SELECT_FUNCTION_TO_BLOCK;
    }
    
    public String getFlag(){
    	//AddressBook book = getAddressBook();
    	//System.out.println("Friends: " + book.getAddressesOfType(AddressType.TRUSTED).size());
    	if(atkCount > 0) {
    		System.out.println("atk: " + atkCount);
    		System.out.println("tru: " + truCount);
    		System.out.println("neu: " + neuCount);
    		atkCount = 0;
    		truCount = 0;
    		neuCount = 0;
    	}
        return TEAM;
    }

    private int getV() {
		try {
			//attempt to read and parse
			return Integer.parseInt(getVariables().get("FuncToReplace"));
		}
		catch(NumberFormatException e) {
			//catch parse errors without crashing
			return -1;
		}
	}
    
	private void cleanVars() {
		//we need to insure we don't accidentally clear out the key:value pair
		//corresponding to our attack target
		List<String> allVars = getVariables().getAll();
		for(String v : allVars) {
			//if it isn't ours, delete it!
			if(!(varList.contains(v))) {
				getVariables().remove(v);
			}
		}
		//clean our own logs too
		getLog().clear();
	}
    
    private int clamp(int v, int i, int j) {
    	if(v < i) v = i;
    	if(v > j) v = j;
		return v;
	}
}
