package Mips;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MMU {
    private int maxReg;
    private int currReg;
    private String[] registers;
    private boolean[] isRegTaken;
    private int currStack;
    private int currStackLevel;

    //Memory Map, from a varName it finds it's register or memory address
    private HashMap<String, MMUTableNode> memoryMap = new HashMap<>();
    private TreeMap<Integer, MMUTableNode> evictionMap = new TreeMap<>();

    //Indicates wether evict algorithm is FIFO or LRU
    private boolean LRU;
    private List<StackSlot> stackSlots = new ArrayList<>();
    private final static String TEM_REG = "$f";
    private final static int MAX_REG = 24;

    public MMU(int maxReg, boolean isLRU) {
        this.maxReg = maxReg;
        this.currReg = 0;
        this.LRU = isLRU;

        this.currStack = 0;
        this.currStackLevel = 0;
        this.registers = new String[maxReg];
        for (int i = 0; i < maxReg && i < MAX_REG; i++) {
            this.registers[i] = TEM_REG + i;
        }
        this.isRegTaken = new boolean[maxReg];
        Arrays.fill(isRegTaken, false);
    }

    //Maps a variable from its name, it'll return either the reg name, or the seqüence of instructions to get it
    public List<String> getVar(String varName, int currLine) {
        if (memoryMap.get(varName) == null) {   //case 1: var is NOT defined yet in mapVar
            return mapVar(varName, currLine);
        } else {                                //case 2: var is defined in mapVar
            return getVarFromTable(varName, currLine);
        }
    }

    private List<String> mapVar(String varName, int currLine) {
        //If var is no defined, first we check if there are available registers
        List<String> instructions = new ArrayList<>();
        String reg = getNextRegister();
        if (reg == null) {      //If there are no registers, we need to perform a swap
            instructions = performSwap(varName);
            MMUTableNode node = new MMUTableNode(varName, instructions.get(instructions.size()-1), null, currLine);
            this.memoryMap.put(varName, node);
            this.evictionMap.put(currLine, node);
        } else {                //Otherwise, we take the register and insert into table
            MMUTableNode node = new MMUTableNode(varName, reg, null, currLine);
            this.memoryMap.put(varName, node);
            this.evictionMap.put(currLine, node);
            instructions.add(reg);
        }
        return instructions;
    }

    private List<String> getVarFromTable(String varName, int currLine) {
        //If var is defined, we check if it's in a register or memory
        List<String> instructions = new ArrayList<>();
        MMUTableNode node = this.memoryMap.get(varName);
        if (this.LRU) {
            if (this.evictionMap.remove(node.getEvictValue()) != null) {
                node.setEvictValue(currLine); //If we're using LRU we MUST update index!
                this.evictionMap.put(currLine, node);
            }
        }
        if (node.getReg() != null) {    //Means value is in register, we can safely return it
            instructions.add(node.getReg());
        } else {                        //Means value is in memory, thus we must perform a swap
            instructions = performSwap(varName);
            //node.setMem(null);
            node.setReg(instructions.get(instructions.size()-1));
            this.evictionMap.put(currLine, node);
        }
        return instructions;
    }

    //Will return list of instructions to perform swap, last position will be register alone
    private List<String> performSwap(String varName) {
        List<String> instructions = new ArrayList<>();

        //1. Get the register (node) to evict --> LOWEST VALUE
        int size = 4;
        MMUTableNode evicted = this.evictionMap.firstEntry().getValue();
        this.evictionMap.remove(this.evictionMap.firstEntry().getKey());

        //2. If it doesn't have an assigned memoryZone, assign one
        if (evicted.getMem() == null) {
            instructions.add("addi $sp, $sp, -4");  //Instruction to expand stack
            String memAddr = allocateStackSlot(evicted.getVarName(), size);
            evicted.setMem(memAddr);
            this.memoryMap.replace(evicted.getVarName(), evicted);
        }

        //3. Perform swap. Move evicted to said memoryZone
        instructions.add("swc1 "+evicted.getReg()+", "+getActualMemoryPos(evicted.getMem()));

        //4. If newVar is in table, means we gotta load memory to evicted's register
        if (this.memoryMap.get(varName) != null) {
            instructions.add("lwc1 "+evicted.getReg()+", "+getActualMemoryPos(this.memoryMap.get(varName).getMem()));
        }

        //5. Free up evicted's register
        instructions.add(evicted.getReg());
        evicted.setReg(null);

        return instructions;
    }

    private String getActualMemoryPos(String input) {
        int index = input.indexOf("($sp)");
        if (index == -1) return input;

        // Find start of the number before ($sp)
        int numEnd = index;
        int numStart = numEnd - 1;

        while (numStart >= 0 && Character.isDigit(input.charAt(numStart))) {
            numStart--;
        }
        numStart++; // step forward to first digit

        String numStr = input.substring(numStart, numEnd);
        int num = Integer.parseInt(numStr);
        int replaced = (currStack-4) - num;

        return input.substring(0, numStart) + replaced + input.substring(numEnd);
    }

    public List<String> saveContext(int scope) {
        List<String> instructions = new ArrayList<>();
        List<MMUTableNode> contextVars = new ArrayList<>();

        //1. How many vars
        for (Map.Entry<String, MMUTableNode> entry : memoryMap.entrySet()) {
            if (entry.getValue().getVarName().endsWith(String.valueOf(scope))) {
                contextVars.add(entry.getValue());
                evictionMap.entrySet().removeIf(entryTree -> {
                    MMUTableNode value = entryTree.getValue();
                    return !value.getVarName().equals(entry.getValue().getVarName());
                });
            }
        }

        //2. Allocate slots for this ones
        this.currStackLevel++;
        for (int i = 0; i < contextVars.size(); i++) {
            contextVars.get(i).setVarName(contextVars.get(i).getVarName()+"_st"+currStackLevel);

            //3. return complete functions
            instructions.add("addi $sp, $sp, -4");
            instructions.add("swc1 "+contextVars.get(i).getReg()+", "+getActualMemoryPos(allocateStackSlot(contextVars.get(i).getVarName(), 4)));
        }

        instructions.add("addi $sp, $sp, -4");
        instructions.add("sw $ra, "+getActualMemoryPos(allocateStackSlot("$ra_"+"_st"+currStackLevel, 4)));

        return instructions;
    }

    public List<String> getContext() {
        List<String> instructions = new ArrayList<>();
        int count = 0;
        boolean canDelete = false;

        for (int i = stackSlots.size() - 1; i > -1; i--) {
            if (stackSlots.get(i).varName != null && stackSlots.get(i).varName.endsWith("_st"+currStackLevel)) {
                if (stackSlots.get(i).offset == (currStack-4)) canDelete = true;
                count++;
                if (stackSlots.get(i).varName.startsWith("$ra")) {
                    instructions.add("lw $ra, " + (currStack -4 - stackSlots.get(i).offset) + "($sp)");
                }else {
                    String varName = stackSlots.get(i).varName.replace("_st" + currStackLevel, "");
                    if (this.memoryMap.get(varName).getReg() == null) {
                        instructions = performSwap(varName);
                    } else {
                        instructions.add("lwc1 " + this.memoryMap.get(varName).getReg() + ", " + (currStack -4 - stackSlots.get(i).offset) + "($sp)");
                    }
                }
                freeStackSlot(stackSlots.get(i).varName);
            }
        }
        if (canDelete) {
            for (int i = 0; i < count; i++) {
                instructions.add(reduceStack());
            }
        }
        currStackLevel--;

        return instructions;
    }

    private String allocateStackSlot(String varName, int size) {
        for (StackSlot slot : stackSlots) {
            if (slot.isFree && slot.size >= size) {
                slot.isFree = false;
                slot.varName = varName;
                return slot.offset + "($sp)";
            }
        }
        // No free slot, allocate new
        int newOffset = currStack;
        stackSlots.add(new StackSlot(newOffset, size, varName));
        currStack += size;
        return newOffset + "($sp)";
    }

    public void freeStackSlot(String varName) {
        for (StackSlot slot : stackSlots) {
            if (slot.varName != null && slot.varName.equals(varName)) {
                slot.isFree = true;
                slot.varName = null;
                break;
            }
        }
    }

    //IMPORTANT: ONLY IF LAST VAR PUT IN STACK WAS RECURSIVITY ONES!!
    private String reduceStack() {
        currStack -= 4;
        stackSlots.remove(stackSlots.size()-1);
        return ("addi $sp, $sp, 4");
    }

    private String getNextRegister() {
        for (int i = 0; i <= maxReg; i++) {      //If we loop maxReg times means there are no freeRegisters
            if (!this.isRegTaken[currReg]) {
                this.isRegTaken[currReg] = true;
                return this.registers[currReg];
            } else {
                currReg++;
                if (currReg >= maxReg) currReg = 0; //Control array bounds :p
            }
        }
        return null;
    }
}
