package codebots.bots;

import codebots.controller.Globals;
import codebots.bot.CodeBot;
import codebots.bot.ReadonlyBot;
import codebots.gameobjects.FunctionType;
import codebots.gameobjects.IPAddress;
import codebots.gameobjects.Message;

public class SwarmBot extends CodeBot {
	
	//treat this as our team string; const-ify it rather than in-lining
	private final String TEAM = "Swarming";
	
	//this is the order of function replacement we wish to cause, makes the set read-only
	//to prevent later modification
	private final List<FunctionType> funcList = new ArrayList<FunctionType>();
	{
		funcList.add(FunctionType.SELECT_FUNCTION_TO_BLOCK);
		funcList.add(FunctionType.SELECT_MESSAGE_RECIPIENTS);
		funcList.add(FunctionType.SEND_MESSAGE);
		funcList.add(FunctionType.GET_FLAG);
		funcList.add(FunctionType.READ_DATA);
		funcList.add(FunctionType.SELECT_FUNCTION_TO_REPLACE);
		funcList.add(FunctionType.SELECT_ATTACK_TARGET);
		funcList.add(FunctionType.PROCESS_MESSAGE);
		funcList = Collections.unmodifiableList(funcList);
	}
	
	//variable entries this code knows about, so we can maliciously remove
	//variables created by other bots
	private final List<String> varList = new ArrayList<String>();
	{
		varList.add("AttackTarget");
		varList.add("FuncToReplace");
		varList.add("MessageType");
		varList = Collections.unmodifiableList(varList);
	}

    @Override
    public IPAddress selectMessageRecipient() {
		cleanVars();
		AddressBook book = getAddressBook();
		//reply to any bots we've identified as friendly
		//TRUSTED bots don't need to be messaged
		l = book.getAddressesOfType(AddressBook.AddressType.UNTRUSTED);
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

	@Override
    public Message sendMessage() {
		cleanVars();
		boolean randMessage = false;
		if(getVariables().has("MessageType")) {
			if(getVariables().get("MessageType").equals("RANDOM")) {
				//messaging an arbitrary bot and tell it to attack a target
				//target chosen uses this class's code, not the *bot's*
				//current selectAttackTarget()
				return new Message(Message.MessageType.ATTACK,this.selectAttackTarget());
			}
		}
		//this is cheating: IPAddress takes a string parameter and makes no attempt
		//to insure that that string is a valid IP address.  Only my processMessage
		//will know how to handle this which prevents other bots spoofing me and
		//gives other bots useless information.  Allows for 2-way confirmation.
        return new Message(Message.MessageType.CONFIRM,new IPAddress(TEAM));
    }

    @Override
    public void processMessage(IPAddress source, Message message) {
		cleanVars();
		AddressBook book = getAddressBook();
		if(message.getType() == Message.MessageType.CONFIRM) {
			//if from ourselves...
			if(message.getAddress().equals(TEAM)) {
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
				if(book.getAddressType(source) == null)
					book.add(source,AddressBook.AddressType.TO_ATTACK);
				if(book.getAddressType(message.getAddress()) == null)
					book.add(message.getAddress(),AddressBook.AddressType.TO_ATTACK);
			}
		}
		else {
			//this address sent me a message, categorize them as neutral
			//also categorize the IP they sent along as neutral
			//assuming we know nothing
			if(book.getAddressType(source) == null)
				book.add(source,AddressBook.AddressType.NEUTRAL);
			if(book.getAddressType(message.getAddress()) == null)
				book.add(message.getAddress(),AddressBook.AddressType.NEUTRAL);
		}
    }
    
    @Override
    public FunctionType selectFunctionToBlock() {
		cleanVars();
		//TODO: fix
		//find a way to determine if this_bot has had its flag altered or not.
		//likely not possible.
		
    	return FunctionType.SELECT_FUNCTION_TO_BLOCK;
    }

    @Override
    public IPAddress selectAttackTarget() {
		cleanVars();
		
		//if only 5% of the game remains, prioritize spreading the flag
		if(getTurnNumber >= Globals.NUM_TURNS_IN_ROUND * 0.95f) {
			l = book.getAddressesOfType(AddressBook.AddressType.TO_ATTACK);
			if(l.size() > 0) {
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
		if(getTurnNumber <= Globals.NUM_TURNS_IN_ROUND * 0.05f) {
			if(getVariables().has("AttackTarget")) {
				String target = getVariables().get("AttackTarget");
				if(getVariables().has(target)) {
					int v = getV();
					if(v < 8 && v >= 0) {
						return new IPAddress(target);
					}
				}
			}
		}
		
		l = book.getAddressesOfType(AddressBook.AddressType.UNTRUSTED);
		if(l.size() > 0) {
			getVariables().add("AttackTarget",l.get(0));
			return l.get(0);
		}
		List<IPAddress> l = book.getAddressesOfType(AddressBook.AddressType.TO_DEFEND);
		if(l.size() > 0) {
			getVariables().add("AttackTarget",l.get(0));
			return l.get(0);
		}
		l = book.getAddressesOfType(AddressBook.AddressType.TO_ATTACK);
		if(l.size() > 0) {
			getVariables().add("AttackTarget",l.get(0));
			return l.get(0);
		}
		l = book.getAddressesOfType(AddressBook.AddressType.NEUTRAL);
		if(l.size() > 0) {
			getVariables().add("AttackTarget",l.get(0));
			return l.get(0);
		}
		l = book.getAddressesOfType(AddressBook.AddressType.TRUSTED);
		if(l.size() > 0) {
			getVariables().add("AttackTarget",l.get(0));
			return l.get(0);
		}
		
		IPAddress rr = book.getAddress(getRandom().nextInt(book.size()));
		getVariables().add("AttackTarget",rr);
        return rr;
    }

    @Override
    public void readData(ReadonlyBot bot) {
		cleanVars();
		AddressBook book = getAddressBook();
		
        ReadonlyAddressBook hisBook = bot.getAddressBook();
        AddressBook.AddressType[] values = AddressBook.AddressType.values();
        for(int i=0;i<values.length;i++){
			//if we don't know anything about the addresses already, store as neutral
			if(book.getAddressType(values[i]) == null) {
				book.addAll(hisBook.getAddressesOfType(values[i]), AddressBook.AddressType.NEUTRAL);
			}
        }
		
    	//TODO: replace (in order) Block, recipient, Flag, message, data, replace, target, process 
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
		book.remove(m.getAddress());
		//largely speaking, they're friendly
    	if(f1 > 3) {
			book.add(m.getAddress(), AddressBook.AddressType.TRUSTED);
			//if their Process Message method is fine (last method replaced)
			if(pm) {
				//they're friendly, don't attack them next turn
				getVariables().remove("AttackTarget");
				//replace semi-random method
				getVariables().add("FuncToReplace",""+(getTurnNumber()%8));
			}
			//not fully converted yet AFAICT
			//if all methods match, skip
			else if(!(mr && sm && at)) {
				int v = getV()+1;
				getVariables().add("FuncToReplace",v);
			}
			else {
				//ever method matches, remove for next turn
				getVariables().remove("AttackTarget");
				//doesn't matter this turn what we do
				getVariables().add("FuncToReplace",""+(getTurnNumber()%8));
			}
    	}
		//partially friendly
		else if(f1 > 0) {
			book.add(m.getAddress(), AddressBook.AddressType.TO_DEFEND);
			if(!fb)
				getVariables().add("FuncToReplace","0");
			else if(!gf)
				getVariables().add("FuncToReplace","3");
			else if(!rd)
				getVariables().add("FuncToReplace","4");
			else if(!fr)
				getVariables().add("FuncToReplace","5");
		}
		//not friendly
    	else {
    		//enemy, neutral, or other
            book.add(m.getAddress(), AddressBook.AddressType.TO_ATTACK);
			//TODO: do in order
			if(getVariables().has("AttackTarget")) {
				String target = getVariables().get("AttackTarget");
				if(getVariables().has(target)) {
					int v = getV()+1;
					getVariables().add("FuncToReplace",v);
				}
				else {
					getVariables().add(target,0);
					getVariables().add("FuncToReplace","0");
				}
			}
			else {
				getVariables().add("FuncToReplace","0");
			}
    	}
		//last 5% of the game, override spread to be flag, unless the flag
		//is already the same
		if(getTurnNumber >= Globals.NUM_TURNS_IN_ROUND * 0.95f && !gf) {
			getVariables().add("FuncToReplace","3");
		}
    }

    @Override
    public FunctionType selectFunctionToReplace() {
		cleanVars();
		//if only 5% of the game remains, prioritize spreading the flag
		if(getTurnNumber >= Globals.NUM_TURNS_IN_ROUND * 0.95f) {
			return FunctionType.GET_FLAG;
		}
		
		//readData() determines what method we want to replace, given the target
		if(getVariables().has("FuncToReplace")) {
			int v = getV();
			if(v != Math.clamp(v,0,7)]) {
				//if the stored value is not valid
				v = getTurnNumber()%8;
			}
			return funcList.get(v);
		}
		//by default, replace selectFunctionToBlock()
        return FunctionType.SELECT_FUNCTION_TO_BLOCK;
    }

    @Override
    public String getFlag() {
		cleanVars();
        return TEAM;
    }
	
	//maliciously remove variable entries that aren't ours
	private void cleanVars() {
		//we need to insure we don't accidentally clear out the key:value pair
		//corresponding to our attack target
		String myAttackTarget = getVariables().get("AttackTarget");
		List<String> allVars = getVariables().getAll();
		for(String v : allVars) {
			//if it isn't ours, delete it!
			if(!(varList.contains(v) || v.equals(myAttackTarget))) {
				getVariables().remove(v);
			}
		}
		//clean our own logs too
		getLog().clear();
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
}
