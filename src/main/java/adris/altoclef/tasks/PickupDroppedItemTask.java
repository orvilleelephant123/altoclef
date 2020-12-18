package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.baritone.GoalGetToPosition;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.csharpisbetter.Util;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.Vec3d;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class PickupDroppedItemTask extends Task {

    private final List<ItemTarget> _itemTargets;

    private AltoClef _mod;

    private final TargetPredicate _targetPredicate = new TargetPredicate();

    private Vec3d _itemGoal;

    public PickupDroppedItemTask(List<ItemTarget> itemTargets) {
        _itemTargets = itemTargets;
        _itemGoal = null;
    }

    public PickupDroppedItemTask(Item item, int targetCount) {
        this(Collections.singletonList(new ItemTarget(item, targetCount)));
    }

    @Override
    protected void onStart(AltoClef mod) {
        _mod = mod;

        // Config
        mod.getConfigState().push();
        mod.getConfigState().setFollowDistance(0);

        // Reset baritone process
        mod.getClientBaritone().getCustomGoalProcess().onLostControl();
    }

    @Override
    protected Task onTick(AltoClef mod) {

        ItemEntity closest = mod.getEntityTracker().getClosestItemDrop(mod.getPlayer().getPos(), Util.toArray(ItemTarget.class, _itemTargets));

        if (!taskAssert(mod, closest != null, "Failed to find any items to pick up. Should have checked this condition earlier")) {
            return null;
        }

        setDebugState("FOUND: " + closest.getStack().getItem().getTranslationKey());

        // These two lines must be paired in this order. path must be called once.
        // Setting goal makes the goal process active, but not pathing! This is undesirable.
        Vec3d goal = closest.getPos();
        if (!mod.getClientBaritone().getCustomGoalProcess().isActive() || _itemGoal == null || _itemGoal.squaredDistanceTo(goal) > 1) {
            Debug.logMessage("(Pickup PATHING");
            //mod.getClientBaritone().getCustomGoalProcess().path();
            mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(new GoalGetToPosition(closest.getPos()));//new GoalGetToPosition(goal));
            _itemGoal = goal;
        }

        //mod.getClientBaritone().getFollowProcess().follow(_targetPredicate);

        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getConfigState().pop();
        // Stop baritone IF the other task isn't an item task.
        if (!(interruptTask instanceof PickupDroppedItemTask)) {
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
            //_mod.getClientBaritone().getFollowProcess().cancel();
            Debug.logMessage("(PICKUP TASK STOPPED)");
        } else {
            Debug.logMessage("(Interrupted by ANOTHER pickup task)");
        }
    }

    @Override
    protected boolean isEqual(Task other) {
        // Same target items
        if (other instanceof PickupDroppedItemTask) {
            PickupDroppedItemTask t = (PickupDroppedItemTask) other;
            if (t._itemTargets.size() != _itemTargets.size()) return false;
            for (int i = 0; i < _itemTargets.size(); ++i) {
                if (!_itemTargets.get(i).equals(t._itemTargets.get(i))) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        StringBuilder result = new StringBuilder();
        result.append("Pickup Dropped Items: [");
        int c = 0;
        for (ItemTarget target : _itemTargets) {
            result.append(target.toString());
            if (++c != _itemTargets.size()) {
                result.append(", ");
            }
        }
        result.append("]");
        return result.toString();
    }

    class TargetPredicate implements Predicate<Entity> {

        @Override
        public boolean test(Entity entity) {
            if (entity instanceof ItemEntity) {
                ItemEntity iEntity = (ItemEntity) entity;
                for (ItemTarget target : _itemTargets) {
                    // If we already have this item, ignore it
                    if (_mod.getInventoryTracker().targetReached(target)) continue;

                    // Match for item
                    if (target.matches(iEntity.getStack().getItem())) {
                        return true;
                    }
                }
            }
            return false;
        }
    }


}