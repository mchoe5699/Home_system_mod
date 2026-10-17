package com.qwerty.homenet.samsung;
import com.qwerty.homenet.network.WallpadDataPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.function.Supplier;
public final class ShtNetwork {
 public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(Samsung.id("controls"),()->"1","1"::equals,"1"::equals);
 public record Data(WallpadDataPacket snapshot,boolean remote){static void encode(Data p,FriendlyByteBuf b){WallpadDataPacket.encode(p.snapshot,b);b.writeBoolean(p.remote);}static Data decode(FriendlyByteBuf b){return new Data(WallpadDataPacket.decode(b),b.readBoolean());}}
 public record Control(BlockPos pos,boolean remote,String key,String value){static void encode(Control p,FriendlyByteBuf b){b.writeBlockPos(p.pos);b.writeBoolean(p.remote);b.writeUtf(p.key,64);b.writeUtf(p.value,256);}static Control decode(FriendlyByteBuf b){return new Control(b.readBlockPos(),b.readBoolean(),b.readUtf(64),b.readUtf(256));}}
 public static void init(){CHANNEL.messageBuilder(Control.class,0,NetworkDirection.PLAY_TO_SERVER).encoder(Control::encode).decoder(Control::decode).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)dispatch(m,p);}).add();CHANNEL.messageBuilder(Data.class,1,NetworkDirection.PLAY_TO_CLIENT).encoder(Data::encode).decoder(Data::decode).consumerMainThread((m,c)->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->ShtClient.receive(m))).add();}
 public static void dispatch(Control m,ServerPlayer p){if(!p.level().isLoaded(m.pos)||!(p.level().getBlockEntity(m.pos) instanceof Sht101c s))return;if(!s.canOperate(p,m.remote)){p.displayClientMessage(Component.literal("기기와 같은 단지·통신 범위 안에서 조작하세요"),true);return;}
  if(m.key.equals("message"))s.message(p,m.value,m.remote);else if(m.key.equals("key"))s.key(m.value,m.remote);else if(m.key.equals("unit")){if(!m.remote)s.handleAction(p,com.qwerty.homenet.blockentity.ReceiverBlockEntity.Action.SET_UNIT,BlockPos.ZERO,m.value);}else if(m.key.equals("logo")){if(!m.remote)s.editLogo("logo_text="+com.qwerty.homenet.intercom.Intercom.sanitize(m.value,32));}else if(m.key.equals("button"))s.press(m.value,p,m.remote);s.changed();}
 public static void data(ServerPlayer p,WallpadDataPacket d,boolean remote){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Data(d,remote));}
 public static void send(BlockPos pos,boolean remote,String key,String value){CHANNEL.sendToServer(new Control(pos,remote,key,value));}
}
