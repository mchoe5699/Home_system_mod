function initializeCoreMod() {
    var Opcodes = Java.type('org.objectweb.asm.Opcodes');
    var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
    var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode');
    var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode');
    var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
    var FrameNode = Java.type('org.objectweb.asm.tree.FrameNode');
    var FieldInsnNode = Java.type('org.objectweb.asm.tree.FieldInsnNode');
    var TypeInsnNode = Java.type('org.objectweb.asm.tree.TypeInsnNode');
    function doorLinkProtocol(method){
        var code=new InsnList(), allowed=new LabelNode();
        code.add(new VarInsnNode(Opcodes.ALOAD,0));code.add(new VarInsnNode(Opcodes.ALOAD,1));
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,'com/qwerty/homenet/sht/DoorDeviceRouting','canLink','(Lcom/qwerty/homenet/blockentity/DoorStationBlockEntity;Lnet/minecraft/core/BlockPos;)Z',false));
        code.add(new JumpInsnNode(Opcodes.IFNE,allowed));code.add(new InsnNode(Opcodes.ICONST_0));code.add(new InsnNode(Opcodes.IRETURN));
        code.add(allowed);code.add(new FrameNode(Opcodes.F_SAME,0,null,0,null));method.instructions.insert(code);method.maxStack=Math.max(method.maxStack,2);return method;
    }
    function doorCallProtocol(method){
        for(var node=method.instructions.getFirst();node!=null;node=node.getNext())if(node.getOpcode()===Opcodes.ARETURN){
            var code=new InsnList();code.add(new VarInsnNode(Opcodes.ALOAD,0));code.add(new InsnNode(Opcodes.SWAP));
            code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,'com/qwerty/homenet/sht/DoorDeviceRouting','compatibleReceivers','(Lcom/qwerty/homenet/blockentity/DoorStationBlockEntity;Ljava/util/List;)Ljava/util/List;',false));
            method.instructions.insertBefore(node,code);
        }
        method.maxStack=Math.max(method.maxStack,2);return method;
    }
    function sameProtocol(method) {
        var code = new InsnList(), reject = new LabelNode();
        code.add(new VarInsnNode(Opcodes.ALOAD,0));
        code.add(new FieldInsnNode(Opcodes.GETSTATIC,'com/qwerty/homenet/guard/GuardSystem','NONE','Lcom/qwerty/homenet/guard/GuardSystem;'));
        code.add(new JumpInsnNode(Opcodes.IF_ACMPEQ,reject));
        code.add(new VarInsnNode(Opcodes.ALOAD,0));
        code.add(new VarInsnNode(Opcodes.ALOAD,2));
        code.add(new JumpInsnNode(Opcodes.IF_ACMPNE,reject));
        code.add(new InsnNode(Opcodes.ICONST_1));code.add(new InsnNode(Opcodes.IRETURN));
        code.add(reject);code.add(new InsnNode(Opcodes.ICONST_0));code.add(new InsnNode(Opcodes.IRETURN));
        method.instructions.clear();method.instructions.add(code);method.maxStack=2;return method;
    }
    function kvmGuardField(method) {
        for (var node = method.instructions.getFirst(); node != null; node = node.getNext()) {
            if (node instanceof FieldInsnNode && node.getOpcode() === Opcodes.GETFIELD && String(node.name) === 'analogStation') {
                method.instructions.set(node, new MethodInsnNode(Opcodes.INVOKESTATIC,
                    'com/qwerty/homenet/sht/GuardRouting', 'kvmStation',
                    '(Lcom/qwerty/homenet/blockentity/Kvm906BlockEntity;)Lnet/minecraft/core/BlockPos;', false));
            }
        }
        return method;
    }
    function kvmLobbyField(method) {
        for (var node = method.instructions.getFirst(); node != null; node = node.getNext()) {
            if (node instanceof FieldInsnNode && node.getOpcode() === Opcodes.GETFIELD && String(node.name) === 'analogStation') {
                method.instructions.insertBefore(node, new VarInsnNode(Opcodes.ALOAD, 1));
                method.instructions.set(node, new MethodInsnNode(Opcodes.INVOKESTATIC,
                    'com/qwerty/homenet/sht/GuardRouting', 'kvmLobbyStation',
                    '(Lcom/qwerty/homenet/blockentity/Kvm906BlockEntity;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/BlockPos;', false));
            }
        }
        method.maxStack = Math.max(method.maxStack, 3);
        return method;
    }
    function numberedKvmSwitchboard(method) {
        var code = new InsnList(), original = new LabelNode();
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new TypeInsnNode(Opcodes.INSTANCEOF, 'com/qwerty/homenet/blockentity/Kvm906BlockEntity'));
        code.add(new JumpInsnNode(Opcodes.IFEQ, original));
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
            'com/qwerty/homenet/sht/GuardRouting', 'kvmSwitchboard',
            '(Lcom/qwerty/homenet/blockentity/ReceiverBlockEntity;)Lcom/qwerty/homenet/guard/GuardExchange;', false));
        code.add(new InsnNode(Opcodes.ARETURN));
        code.add(original);
        code.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        method.instructions.insert(code);
        method.maxStack = Math.max(method.maxStack, 1);
        return method;
    }
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
    function replaceCall(method, owner, name, descriptor, helper, replacement) {
        for (var node = method.instructions.getFirst(); node != null; node = node.getNext()) {
            if (node instanceof MethodInsnNode && String(node.owner) === owner && String(node.name) === name && String(node.desc) === descriptor) {
                method.instructions.set(node, new MethodInsnNode(Opcodes.INVOKESTATIC,
                    'com/qwerty/homenet/sht/GuardRouting', helper, replacement, false));
                break;
            }
        }
        return method;
    }
    function routeGuard(method) {
        for (var node = method.instructions.getFirst(); node != null; node = node.getNext()) {
            if (node instanceof FieldInsnNode && String(node.name) === 'analogStation') {
                method.instructions.insertBefore(node, new VarInsnNode(Opcodes.ALOAD, 2));
                method.instructions.set(node, new MethodInsnNode(Opcodes.INVOKESTATIC,
                    'com/qwerty/homenet/sht/GuardRouting', 'linkedForCall',
                    '(Lcom/qwerty/homenet/blockentity/ReceiverBlockEntity;Ljava/lang/String;)Lnet/minecraft/core/BlockPos;', false));
                break;
            }
        }
        var code = new InsnList();
        var original = new LabelNode();
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new VarInsnNode(Opcodes.ALOAD, 1));
        code.add(new VarInsnNode(Opcodes.ALOAD, 2));
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, 'com/qwerty/homenet/sht/GuardRouting',
            'routePreferredCall', '(Lcom/qwerty/homenet/blockentity/ReceiverBlockEntity;Lnet/minecraft/server/level/ServerPlayer;Ljava/lang/String;)Z', false));
        code.add(new JumpInsnNode(Opcodes.IFEQ, original));
        code.add(new InsnNode(Opcodes.RETURN));
        code.add(original);
        code.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        method.instructions.insert(code);
        method.maxStack = Math.max(method.maxStack, 3);
        return replaceCall(method,
            'com/qwerty/homenet/data/DeviceRegistry', 'guardsByNumber',
            '(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Ljava/lang/String;)Ljava/util/List;',
            'numberedGuardList',
            '(Lcom/qwerty/homenet/data/DeviceRegistry;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Ljava/lang/String;)Ljava/util/List;');
    }
    function preventLobbyFallback(method) {
        var seen = 0;
        for (var node = method.instructions.getFirst(); node != null; node = node.getNext()) {
            if (node instanceof MethodInsnNode && String(node.name) === 'receiversOf'
                    && String(node.desc) === '(Ljava/lang/String;)Ljava/util/List;') {
                if (++seen === 2) {
                    method.instructions.set(node, new MethodInsnNode(Opcodes.INVOKESTATIC,
                        'com/qwerty/homenet/sht/GuardRouting', 'noLobbyFallback',
                        '(Lcom/qwerty/homenet/blockentity/LobbyPhoneBlockEntity;Ljava/lang/String;)Ljava/util/List;', false));
                    break;
                }
            }
        }
        return method;
    }
    function selectedLobbyGuardReceivers(method) {
        for(var node=method.instructions.getFirst();node!=null;node=node.getNext())if(node.getOpcode()===Opcodes.ARETURN){
            var filter=new InsnList();filter.add(new VarInsnNode(Opcodes.ALOAD,0));filter.add(new InsnNode(Opcodes.SWAP));
            filter.add(new MethodInsnNode(Opcodes.INVOKESTATIC,'com/qwerty/homenet/sht/GuardRouting','lobbyApartmentReceivers','(Lcom/qwerty/homenet/blockentity/CallerBlockEntity;Ljava/util/List;)Ljava/util/List;',false));
            method.instructions.insertBefore(node,filter);
        }
        var code = new InsnList(), original = new LabelNode();
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new TypeInsnNode(Opcodes.INSTANCEOF, 'com/qwerty/homenet/blockentity/LobbyPhoneBlockEntity'));
        code.add(new JumpInsnNode(Opcodes.IFEQ, original));
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new TypeInsnNode(Opcodes.CHECKCAST, 'com/qwerty/homenet/blockentity/LobbyPhoneBlockEntity'));
        code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, 'com/qwerty/homenet/blockentity/LobbyPhoneBlockEntity', 'isGuardCall', '()Z', false));
        code.add(new JumpInsnNode(Opcodes.IFEQ, original));
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new VarInsnNode(Opcodes.ALOAD, 1));
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, 'com/qwerty/homenet/sht/GuardRouting', 'lobbyGuardReceivers', '(Lcom/qwerty/homenet/blockentity/CallerBlockEntity;Ljava/lang/String;)Ljava/util/List;', false));
        code.add(new InsnNode(Opcodes.ARETURN));
        code.add(original);
        code.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        method.instructions.insert(code);
        method.maxStack = Math.max(method.maxStack, 2);
        return method;
    }
    function includeHt100Cameras(method) {
        for (var node = method.instructions.getFirst(); node != null; ) {
            var next = node.getNext();
            if (node.getOpcode() === Opcodes.ARETURN) {
                var code = new InsnList();
                code.add(new VarInsnNode(Opcodes.ALOAD, 0));
                code.add(new InsnNode(Opcodes.SWAP));
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                    'com/qwerty/homenet/sht/DoorCameraRouting', 'linked',
                    '(Lcom/qwerty/homenet/blockentity/ReceiverBlockEntity;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/BlockPos;', false));
                method.instructions.insertBefore(node, code);
            }
            node = next;
        }
        method.maxStack = Math.max(method.maxStack, 2);
        return method;
    }
    return {
        'home_linker_samsung_door_phone': {
            target: {type:'METHOD', class:'com.qwerty.homenet.item.HomeLinkerItem', methodName:'onItemUseFirst', methodDesc:'(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;'},
            transformer: function(method) {
                var code=new InsnList(), original=new LabelNode();
                code.add(new VarInsnNode(Opcodes.ALOAD,1));code.add(new VarInsnNode(Opcodes.ALOAD,2));
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,'com/qwerty/homenet/sht/Ht100sLinker','use','(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;',false));
                code.add(new InsnNode(Opcodes.DUP));
                code.add(new FieldInsnNode(Opcodes.GETSTATIC,'net/minecraft/world/InteractionResult','PASS','Lnet/minecraft/world/InteractionResult;'));
                code.add(new JumpInsnNode(Opcodes.IF_ACMPEQ,original));code.add(new InsnNode(Opcodes.ARETURN));
                code.add(original);code.add(new FrameNode(Opcodes.F_SAME1,0,null,1,['net/minecraft/world/InteractionResult']));code.add(new InsnNode(Opcodes.POP));
                method.instructions.insert(code);method.maxStack=Math.max(method.maxStack,2);return method;
            }
        },
        'door_linker_lobby_phone': {
            target: {type:'METHOD', class:'com.qwerty.homenet.item.DoorLinkerItem', methodName:'onItemUseFirst', methodDesc:'(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;'},
            transformer: function(method) {
                var code=new InsnList(), original=new LabelNode();
                code.add(new VarInsnNode(Opcodes.ALOAD,1));code.add(new VarInsnNode(Opcodes.ALOAD,2));
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,'com/qwerty/homenet/sht/DoorCameraRouting','lobbyDoor','(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;',false));
                code.add(new InsnNode(Opcodes.DUP));
                code.add(new FieldInsnNode(Opcodes.GETSTATIC,'net/minecraft/world/InteractionResult','PASS','Lnet/minecraft/world/InteractionResult;'));
                code.add(new JumpInsnNode(Opcodes.IF_ACMPEQ,original));code.add(new InsnNode(Opcodes.ARETURN));
                code.add(original);code.add(new FrameNode(Opcodes.F_SAME1,0,null,1,['net/minecraft/world/InteractionResult']));code.add(new InsnNode(Opcodes.POP));
                method.instructions.insert(code);method.maxStack=Math.max(method.maxStack,2);return method;
            }
        },
        'functional_sht5500s': {
            target: {type:'METHOD', class:'com.qwerty.homenet.block.Sht5500sBlock', methodName:'newBlockEntity', methodDesc:'(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/entity/BlockEntity;'},
            transformer: function(method) {
                var owner='com/qwerty/homenet/sht/Sht5500s';
                method.instructions.clear();
                method.instructions.add(new TypeInsnNode(Opcodes.NEW,owner));
                method.instructions.add(new InsnNode(Opcodes.DUP));
                method.instructions.add(new VarInsnNode(Opcodes.ALOAD,1));
                method.instructions.add(new VarInsnNode(Opcodes.ALOAD,2));
                method.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,owner,'<init>','(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V',false));
                method.instructions.add(new InsnNode(Opcodes.ARETURN));
                method.maxStack=4;
                return method;
            }
        },
        'tick_sht5500s_calls': {
            target: {type:'METHOD', class:'com.qwerty.homenet.block.Sht5500sBlock', methodName:'getTicker', methodDesc:'(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntityType;)Lnet/minecraft/world/level/block/entity/BlockEntityTicker;'},
            transformer: function(method) {
                method.instructions.clear();
                method.instructions.add(new VarInsnNode(Opcodes.ALOAD,1));
                method.instructions.add(new VarInsnNode(Opcodes.ALOAD,2));
                method.instructions.add(new VarInsnNode(Opcodes.ALOAD,3));
                method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,'com/qwerty/homenet/sht/Sht5500s','ticker','(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntityType;)Lnet/minecraft/world/level/block/entity/BlockEntityTicker;',false));
                method.instructions.add(new InsnNode(Opcodes.ARETURN));
                method.maxStack=3;
                return method;
            }
        },
        'include_addon_products': {
            target: {type:'METHOD', class:'com.qwerty.homenet.move.DeviceMoves', methodName:'product', methodDesc:'(Lnet/minecraft/world/level/block/state/BlockState;)Z'},
            transformer: function(method) { return extend(method, 'additionalProduct', '(Lnet/minecraft/world/level/block/state/BlockState;)Z'); }
        },
        'preserve_active_samsung_calls': {
            target: {type:'METHOD', class:'com.qwerty.homenet.move.DeviceMoves', methodName:'busy', methodDesc:'(Lnet/minecraft/world/level/block/entity/BlockEntity;)Z'},
            transformer: function(method) { return extend(method, 'additionalBusy', '(Lnet/minecraft/world/level/block/entity/BlockEntity;)Z'); }
        },
        'zone_scoped_lobby_guard_preference': {
            target: {type:'METHOD', class:'com.qwerty.homenet.blockentity.LobbyPhoneBlockEntity', methodName:'startGuardCall', methodDesc:'()V'},
            transformer: function(method) { return preventLobbyFallback(replaceCall(method,
                'com/qwerty/homenet/blockentity/LobbyPhoneBlockEntity', 'cfg', '(Ljava/lang/String;)Ljava/lang/String;',
                'lobbyGuardNumber', '(Lcom/qwerty/homenet/blockentity/LobbyPhoneBlockEntity;Ljava/lang/String;)Ljava/lang/String;')); }
        },
        'selected_lobby_guard_only': {
            target: {type:'METHOD', class:'com.qwerty.homenet.blockentity.CallerBlockEntity', methodName:'receiversOf', methodDesc:'(Ljava/lang/String;)Ljava/util/List;'},
            transformer: selectedLobbyGuardReceivers
        },
        'dashboard_installed_devices_only': {
            target: {type:'METHOD', class:'com.qwerty.homenet.item.DashboardItem', methodName:'sendData', methodDesc:'(Lnet/minecraft/server/level/ServerPlayer;Z)V'},
            transformer: function(method){
                for(var node=method.instructions.getFirst();node!=null;node=node.getNext())if(node instanceof MethodInsnNode&&String(node.owner)==='com/qwerty/homenet/data/DeviceRegistry'&&String(node.name)==='all'){
                    method.instructions.insertBefore(node,new VarInsnNode(Opcodes.ALOAD,2));
                    method.instructions.set(node,new MethodInsnNode(Opcodes.INVOKESTATIC,'com/qwerty/homenet/sht/GuardRouting','installedEntries','(Lcom/qwerty/homenet/data/DeviceRegistry;Lnet/minecraft/server/level/ServerLevel;)Ljava/util/List;',false));break;
                }
                method.maxStack=Math.max(method.maxStack,6);return method;
            }
        },
        'random_initial_apartment_number': {
            target: {type:'METHOD', class:'com.qwerty.homenet.blockentity.ReceiverBlockEntity', methodName:'bindSwitchboard', methodDesc:'(Lnet/minecraft/core/BlockPos;)Z'},
            transformer: function(method) { return replaceCall(method,
                'com/qwerty/homenet/guard/GuardUnitAssignments', 'allocate',
                '(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Z)Ljava/lang/String;',
                'randomUnit', '(Lcom/qwerty/homenet/guard/GuardUnitAssignments;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Z)Ljava/lang/String;'); }
        },
        'door_link_protocol': {target:{type:'METHOD',class:'com.qwerty.homenet.blockentity.DoorStationBlockEntity',methodName:'toggleReceiver',methodDesc:'(Lnet/minecraft/core/BlockPos;)Z'},transformer:doorLinkProtocol},
        'door_call_protocol': {target:{type:'METHOD',class:'com.qwerty.homenet.blockentity.DoorStationBlockEntity',methodName:'receiversOf',methodDesc:'(Ljava/lang/String;)Ljava/util/List;'},transformer:doorCallProtocol},
        'protocol_only_compatibility': {
            target: {type:'METHOD', class:'com.qwerty.homenet.guard.GuardSystem', methodName:'compatible', methodDesc:'(Ljava/lang/String;Lcom/qwerty/homenet/guard/GuardSystem;Ljava/lang/String;)Z'},
            transformer: sameProtocol
        },
        'single_destination_guard_call': {
            target: {type:'METHOD', class:'com.qwerty.homenet.blockentity.ReceiverBlockEntity', methodName:'callGuard', methodDesc:'(Lnet/minecraft/server/level/ServerPlayer;Ljava/lang/String;)V'},
            transformer: function(method) { return routeGuard(method); }
        },
        'kocom_numbered_guard_switchboard': {
            target: {type:'METHOD', class:'com.qwerty.homenet.blockentity.ReceiverBlockEntity', methodName:'switchboard', methodDesc:'()Lcom/qwerty/homenet/guard/GuardExchange;'},
            transformer: numberedKvmSwitchboard
        },
        'kocom_numbered_guard_call': {
            target: {type:'METHOD', class:'com.qwerty.homenet.blockentity.Kvm906BlockEntity', methodName:'callGuard', methodDesc:'(Lnet/minecraft/server/level/ServerPlayer;Ljava/lang/String;)V'},
            transformer: function(method) { return routeGuard(kvmGuardField(method)); }
        },
        'kocom_numbered_guard_session': {
            target: {type:'METHOD', class:'com.qwerty.homenet.blockentity.Kvm906BlockEntity', methodName:'beginSwitchboardCall', methodDesc:'(Lnet/minecraft/core/BlockPos;)Z'},
            transformer: kvmGuardField
        },
        'kocom_lobby_without_guard_link': {
            target: {type:'METHOD', class:'com.qwerty.homenet.blockentity.Kvm906BlockEntity', methodName:'startRinging', methodDesc:'(Lnet/minecraft/core/BlockPos;Ljava/lang/String;)Z'},
            transformer: kvmLobbyField
        },
        'kocom_lobby_permission_without_guard_link': {
            target: {type:'METHOD', class:'com.qwerty.homenet.blockentity.Kvm906BlockEntity', methodName:'lobbyAllowed', methodDesc:'(Lnet/minecraft/core/BlockPos;)Z'},
            transformer: kvmLobbyField
        },
        'ha4100h_selectable_guard_system': {
            target: {type:'METHOD', class:'com.qwerty.homenet.blockentity.Ha4100hBlockEntity', methodName:'guardSystem', methodDesc:'()Lcom/qwerty/homenet/guard/GuardSystem;'},
            transformer: function(method){var code=new InsnList();code.add(new VarInsnNode(Opcodes.ALOAD,0));code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,'com/qwerty/homenet/sht/GuardRouting','haSystem','(Lcom/qwerty/homenet/blockentity/Ha4100hBlockEntity;)Lcom/qwerty/homenet/guard/GuardSystem;',false));code.add(new InsnNode(Opcodes.ARETURN));method.instructions.clear();method.instructions.add(code);method.maxStack=1;return method;}
        },
        'ha4100h_selectable_analog_bus': {
            target: {type:'METHOD', class:'com.qwerty.homenet.blockentity.Ha4100hBlockEntity', methodName:'analogBus', methodDesc:'()Lcom/qwerty/homenet/analog/AnalogBus;'},
            transformer: function(method){var code=new InsnList();code.add(new VarInsnNode(Opcodes.ALOAD,0));code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,'com/qwerty/homenet/sht/GuardRouting','haBus','(Lcom/qwerty/homenet/blockentity/Ha4100hBlockEntity;)Lcom/qwerty/homenet/analog/AnalogBus;',false));code.add(new InsnNode(Opcodes.ARETURN));method.instructions.clear();method.instructions.add(code);method.maxStack=1;return method;}
        },
        'ht100_video_camera_link': {
            target: {type:'METHOD', class:'com.qwerty.homenet.blockentity.ReceiverBlockEntity', methodName:'linkedCamera', methodDesc:'()Lnet/minecraft/core/BlockPos;'},
            transformer: function(method) { return includeHt100Cameras(method); }
        }
    };
}
