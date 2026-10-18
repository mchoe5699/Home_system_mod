function initializeCoreMod() {
    var Opcodes = Java.type('org.objectweb.asm.Opcodes');
    var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
    var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode');
    var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode');
    var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
    var FrameNode = Java.type('org.objectweb.asm.tree.FrameNode');
    function extend(method, helper, descriptor) {
        var code = new InsnList();
        var original = new LabelNode();
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, 'com/qwerty/homenet/sht/MovementSupport', helper, descriptor, false));
        code.add(new JumpInsnNode(Opcodes.IFEQ, original));
        code.add(new InsnNode(Opcodes.ICONST_1));
        code.add(new InsnNode(Opcodes.IRETURN));
        code.add(original);
        code.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        method.instructions.insert(code);
        method.maxStack = Math.max(method.maxStack, 1);
        return method;
    }
    return {
        'include_addon_products': {
            target: {type:'METHOD', class:'com.qwerty.homenet.move.DeviceMoves', methodName:'product', methodDesc:'(Lnet/minecraft/world/level/block/state/BlockState;)Z'},
            transformer: function(method) { return extend(method, 'additionalProduct', '(Lnet/minecraft/world/level/block/state/BlockState;)Z'); }
        },
        'preserve_active_samsung_calls': {
            target: {type:'METHOD', class:'com.qwerty.homenet.move.DeviceMoves', methodName:'busy', methodDesc:'(Lnet/minecraft/world/level/block/entity/BlockEntity;)Z'},
            transformer: function(method) { return extend(method, 'additionalBusy', '(Lnet/minecraft/world/level/block/entity/BlockEntity;)Z'); }
        }
    };
}
