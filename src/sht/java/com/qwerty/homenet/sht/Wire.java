package com.qwerty.homenet.sht;
import com.qwerty.homenet.network.WallpadDataPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
public class Wire {
 static final net.minecraftforge.network.simple.SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(Product.id("station"),()->"1","1"::equals,"1"::equals);
 public record Input(BlockPos pos,boolean remote,String action,String value){static void encode(Input p,FriendlyByteBuf b){b.writeBlockPos(p.pos);b.writeBoolean(p.remote);b.writeUtf(p.action,32);b.writeUtf(p.value,160);}static Input decode(FriendlyByteBuf b){return new Input(b.readBlockPos(),b.readBoolean(),b.readUtf(32),b.readUtf(160));}}
 record View(WallpadDataPacket state,boolean remote){static void encode(View p,FriendlyByteBuf b){WallpadDataPacket.encode(p.state,b);b.writeBoolean(p.remote);}static View decode(FriendlyByteBuf b){return new View(WallpadDataPacket.decode(b),b.readBoolean());}}
 static void init(){CHANNEL.messageBuilder(Input.class,0,NetworkDirection.PLAY_TO_SERVER).encoder(Input::encode).decoder(Input::decode).consumerMainThread((p,c)->{var player=c.get().getSender();if(player!=null)apply(p,player);}).add();CHANNEL.messageBuilder(View.class,1,NetworkDirection.PLAY_TO_CLIENT).encoder(View::encode).decoder(View::decode).consumerMainThread((p,c)->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->Client.receive(p))).add();}
 public static void apply(Input in,ServerPlayer p){if(!p.level().isLoaded(in.pos)||!(p.level().getBlockEntity(in.pos) instanceof Station s)||!s.allowed(p,in.remote))return;switch(in.action){case "press"->s.press(in.value,p,in.remote);case "digit"->s.digit(in.value,in.remote);case "chat"->s.say(p,in.value,in.remote);case "unit"->{if(!in.remote)s.setUnit(p,in.value);}case "logo"->{if(!in.remote)s.editLogo("logo_text="+com.qwerty.homenet.intercom.Intercom.sanitize(in.value,32));}}s.update();}
 static void show(ServerPlayer p,WallpadDataPacket data,boolean remote){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new View(data,remote));}
 static void send(BlockPos p,boolean remote,String action,String value){CHANNEL.sendToServer(new Input(p,remote,action,value));}
}
