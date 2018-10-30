package game;

import util.FixedCapacityList;

import java.text.BreakIterator;

import static game.Tile.*;

/**
 * The monster list. The list attribute is the actual list.
 */
public class CreatureList{

    private static int NO_DIRECTION = -1;
    
    private Level level;

    public Creature[] list;
    int numDeadMonsters;
    private FixedCapacityList<Creature> newClones;
    public static int direction;
    boolean blobStep;
    
    public Creature creatureAt(Position position){
        int index = position.getIndex();
        for (Creature c : list) if (c.getIndex() == index) return c;
        return null;
    }

    void tick(){

        blobStep = (level.getStep() == Step.EVEN) != (level.tickN % 4 == 3);

        direction = NO_DIRECTION;
        for(Creature monster : list){

            if (monster.isBlock()){
                numDeadMonsters++;
                continue;
            }
            if (!blobStep && (monster.getMonsterType() == Creature.TEETH || monster.getMonsterType() == Creature.BLOB)){
                continue;
            }

            if (!monster.isSliding()){
                if (!monster.isAffectedByCB()) direction = monster.getDirection();
                Tile bgTile = level.layerBG[monster.getIndex()];
                if (bgTile == CLONE_MACHINE) tickClonedMonster(monster);
                else if (bgTile == TRAP) tickTrappedMonster(monster);
                else tickFreeMonster(monster);
            }
        }
    }

    private void tickClonedMonster(Creature monster){
        int clonerPosition = monster.getIndex();
        Tile tile = monster.toTile();
        if (monster.isBlock()) tile = Tile.fromOrdinal(BLOCK_UP.ordinal() + (monster.getDirection() >>> 14));
        if (!monster.isAffectedByCB()) direction = monster.getDirection();
        if (direction == NO_DIRECTION) return;
        if (monster.canEnter(direction, level.layerFG[monster.move(direction).getIndex()], level)){
            if (monster.getMonsterType() == Creature.BLOB){
                int[] directions = monster.getDirectionPriority(level.getChip(), level.rng);
                monster.tick(directions, level);
            }
            else monster.tick(new int[] {direction}, level);
            level.insertTile(clonerPosition, tile);
        }
    }

    private void tickTrappedMonster(Creature monster){
        if (!monster.isAffectedByCB()) direction = monster.getDirection();
        if (direction == NO_DIRECTION) return;
        if (monster.getMonsterType() == Creature.TANK_STATIONARY) monster.setMonsterType(Creature.TANK_MOVING);
        if (monster.getMonsterType() == Creature.BLOB){
            int[] directions = monster.getDirectionPriority(level.getChip(), level.rng);
            monster.tick(directions, level);
        }
        else monster.tick(new int[] {direction}, level);
    }

    private void tickFreeMonster(Creature monster){
        int[] directionPriorities = monster.getDirectionPriority(level.getChip(), level.rng);
        monster.tick(directionPriorities, level);
    }

    void addClone(int position){

        for (Creature c: list){
            if (c.getIndex() == position) return;
        }
        Tile tile = level.layerFG[position];
        if (!tile.isCreature()) return;
        Creature clone = new Creature(position, tile);
        direction = clone.getDirection();
        Position newPosition = clone.move(direction);
        Tile newTile = level.layerFG[newPosition.getIndex()];

        if (clone.canEnter(direction, newTile, level) || newTile == clone.toTile()){
            if (clone.isBlock()) tickClonedMonster(clone);
            else newClones.add(clone);
        }

    }

    void finalise(){

        int length = list.length - numDeadMonsters + newClones.size();
        Creature[] newMonsterList = new Creature[length];

        // Re-add everything except dead monsters. Non-sliding blocks count as dead.
        int index = 0;
        for (Creature monster : list){
            if (!monster.isDead() && !(monster.isBlock() && !monster.isSliding())) newMonsterList[index++] = monster;
        }

        // Add all cloned monsters
        for (Creature clone : newClones){
            newMonsterList[index++] = clone;
        }

        list = newMonsterList;
        newClones.clear();
        numDeadMonsters = 0;

    }

    void setLevel(Level level){
        this.level = level;
        newClones = new FixedCapacityList<>(this.level.cloneConnections.length);
    }

    public CreatureList(Creature[] monsters){
        list = monsters;
        numDeadMonsters = 0;
    }

}
