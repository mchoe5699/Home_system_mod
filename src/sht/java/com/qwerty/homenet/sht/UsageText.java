package com.qwerty.homenet.sht;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import com.qwerty.homenet.client.ReceiverScreen;
import com.qwerty.homenet.blockentity.Kvm906BlockEntity;
import com.qwerty.homenet.kvm.KvmModel;

/** Instructions for implemented controls, using the selected family model rather than the block item. */
final class UsageText {
 private UsageText(){}
 static KvmModel kvm(Screen s){var mc=Minecraft.getInstance();return s instanceof ReceiverScreen r&&mc.level!=null&&mc.level.getBlockEntity(r.getPos()) instanceof Kvm906BlockEntity b?b.design().model():null;}
 static String title(Screen s){var model=kvm(s);return model!=null?model.label():s.getTitle().getString();}
 static List<String> forScreen(Screen s){var out=new ArrayList<String>();String kind=s.getClass().getSimpleName();out.add("["+title(s)+"]");
  if(s instanceof AddressControls){out.add("SHT-5500S는 현재 외형 모델과 동·호 설정만 지원합니다. 동-호를 입력한 뒤 동·호 저장을 누르세요. 통화·문열림 기능은 아직 지원하지 않습니다.");return out;}
  if(s instanceof PortableControls){out.add("[연결] 휴대장치를 충전기에 놓고 홈 링커로 무선 지원 삼성 세대내 본체와 충전기를 차례로 선택합니다. 세대외부기기에 직접 연결하지 않습니다.");out.add("[착신·통화] 연결된 본체로 온 현관·공동현관·경비실 호출을 함께 받습니다. 통화 버튼 또는 충전기에서 받기로 응답하고, 종료 키 또는 충전기에 걸기로 끊습니다.");out.add("[현관·문열림] 현관 키로 현관을 확인합니다. 현관 또는 공동현관과 통화 중 *을 누르면 해당 문을 엽니다. 휴대장치는 음성 전용이며 영상 화면이 없습니다.");out.add("[내선] 내선으로 본체와 통화하거나 외부 통화를 본체로 넘깁니다. 본체와의 내선 호출에 응답하세요.");out.add("[전원·충전] 휴대장치를 Shift+사용하면 전원을 전환합니다. 충전기에 놓아 충전하세요. 전원이 켜진 장치를 소지하고 같은 단지·차원에서 본체 32블록 이내에 있어야 합니다.");}
  else if(s instanceof Controls c){out.add("[설치·주소] BUS / STAR로 방식을 선택하고 설정칸에 동-호(예: 101-1001)를 입력한 뒤 동·호 설정을 누릅니다. BUS는 디지털, STAR는 아날로그이며 연결 가능한 외부기기가 다릅니다. 이 게임에서는 두 방식 모두 본체에서 주소를 직접 설정합니다.");out.add("[연결] 홈 링커로 본체와 세대현관 도어폰·도어카메라를 연결합니다. 공동현관·경비실은 같은 단지의 주소와 통신 방식이 맞아야 합니다. 문은 도어 링커로 외부기기와 연결합니다.");out.add("[착신] 호출이 오면 수화기 공간 또는 수화기 들기 버튼으로 받습니다. 수화기 공간을 다시 누르면 걸고 종료합니다.");
   if(c.wired()){out.add("[현관·문열림] SHT-102C에는 별도 문열림 버튼이 없습니다. 현관 버튼으로 현관 영상을 확인합니다. 현관·공동현관 통화 중 *을 누르면 해당 문이 열립니다. BUS 로비폰 착신 → 수화기 들기 → * 순서입니다.");out.add("[내선·국선] 내선은 연동된 휴대장치와의 통화에 사용합니다. 국선 버튼은 문열림 버튼이 아닙니다.");}
   else{out.add("[현관·문열림] 대기 중 문/열림은 세대현관 영상을 보여줍니다. 외부기기를 호출하는 동작이 아닙니다. 연결된 현관·공동현관 통화에서 문/열림을 누르면 해당 문을 엽니다.");out.add("[스피커] 스피커 버튼으로 수화기를 거치한 채 착신을 받고 통화할 수 있습니다. 다시 누르면 종료합니다.");}
   out.add("[방범] 비상·방범·외출과 시험 버튼을 누르면 해당 상태가 표시됩니다. 경보정지는 소리를 멈추고, 복귀는 경보 상태를 해제합니다. 비상 버튼 자체에는 조명이 켜지지 않습니다.");out.add("[휴대장치] 무선 지원 삼성 본체와 충전기를 홈 링커로 연결하면 공통 휴대장치가 본체의 외부 착신을 함께 받습니다.");if(c.radio)out.add("[무선수화기] 본체에 등록된 수화기로 통화합니다. 본체의 빈 거치 공간을 눌러 다시 걸 수 있습니다.");}
  else if(kvm(s)!=null){kvmText(kvm(s),out);}
  else if(kind.equals("Klp203rfScreen")||kind.equals("LobbyPhoneScreen")){out.add("[세대 호출] 화면의 숫자키로 세대 번호를 입력하고 호출을 누릅니다. 취소로 입력·호출을 취소합니다. 동 입력 방식은 해당 모델의 동/호 표시를 따릅니다.");out.add("[경비·출입] 경비 키가 있는 모델은 등록된 경비실로 호출합니다. 비밀번호·카드 출입은 해당 모델의 등록된 기능만 사용합니다. 출입 비밀번호와 카드 등록은 관리자 설정에서 관리합니다.");out.add("[문 연결] 도어 링커로 이 공동현관기와 문을 연결하세요. 세대가 응답하여 문을 열거나 유효한 출입 인증을 하면 연결된 문이 열립니다.");out.add("[설정] 로비 번호·동·경비 번호와 세대내 기기의 번호·방식을 맞추세요. 모델 변경 화면에서 고른 제품에 따라 키와 사용 가능한 기능이 달라집니다.");}
  else if(kind.equals("DoorStationScreen")){out.add("[세대현관] 홈 링커로 세대내 수신기를 선택한 뒤 이 도어폰·카메라를 선택합니다. 호출 버튼을 누르면 연결된 세대로 호출합니다.");out.add("[공동현관] 세대 지정 없이 사용하는 공동현관 모드에서는 세대 번호를 입력하여 호출합니다. 설정에서 현재 현관 모드와 연결 세대를 확인하세요.");out.add("[통화·출입] 상대가 응답한 뒤 메시지로 대화합니다. 도어 링커로 문을 연결하면 세대내 기기의 문열림 조작으로 열 수 있습니다. 카메라가 있는 모델에서만 영상이 전송됩니다.");}
  else if(kind.equals("GuardMasterScreen")||kind.equals("Kam305Screen")||kind.equals("Am10kpScreen")){out.add("[등록] 설정에서 경비실 번호와 연결 방식을 맞춥니다. 같은 단지의 호환 세대내 기기·공동현관기와 통화할 수 있습니다.");out.add("[통화] 수화기로 착신을 받고 다시 걸어 종료합니다. 세대·경비·로비 선택 키가 있는 모델은 원하는 대상을 선택한 뒤 번호를 입력하여 호출합니다. 확인 키가 있는 화면은 확인으로 발신합니다.");out.add("[문열림] 공동현관과 통화할 때 문열림 키로 해당 출입문을 엽니다. 외부기기와 문은 도어 링커로 연결되어 있어야 합니다.");if(kind.equals("GuardMasterScreen"))out.add("[KGP-70K] 세대는 동 → 확인 → 호 → 확인, 경비·로비는 번호 → 조그 확인 순서입니다. *은 입력 취소, 휠은 목록 이동입니다. 설정의 관리자모드에서 경비 주소를 지정하고 조회에서 호출 기록을 확인합니다.");out.add("[비상] 세대의 비상·방범 알림을 확인하고 경보정지·복귀 키가 있는 모델은 해당 키로 처리합니다.");}
  else if(kind.equals("KhnWallpadScreen")||kind.equals("WallpadScreen")){out.add("[주소·연결] 설정에서 세대 번호를 지정합니다. KHN-893N은 설정 → 관리자모드(기본 비밀번호 9999)에서 동·호와 관할 경비실을 등록합니다. 홈 링커로 세대현관과 제어 기기를 연결합니다.");out.add("[통화] 착신의 응답·거절·문열기 또는 본체 통화 키를 사용합니다. 현관·공동현관 통화에서 문열림으로 해당 문을 엽니다. 통화 메뉴에서 지원되는 세대·경비실 통화를 선택합니다.");out.add("[제어·조회] 제어 메뉴에서 연결된 조명·난방·가스 등을 조작하고, 조회에서 방문자와 부재중 기록을 확인합니다. 화면에 표시되는 기능은 기기 종류에 따라 다릅니다.");}
  else if(kind.equals("Ip203apScreen")||kind.equals("Hmc7000Screen")||kind.equals("Ha4100hConfigScreen")){out.add("[설치] 기기의 세대 번호와 연결 방식을 맞추고, 홈 링커로 세대현관을 연결합니다. 교환기 방식 제품은 호환 교환기와 연결하여 세대 주소를 설정합니다.");out.add("[통화] 호출이 오면 수화기를 들어 응답하고 거치 공간을 눌러 종료합니다. 화면에 표시된 현관·경비·내선 키로 대상을 선택합니다. 숫자키가 있는 모델은 번호 입력 후 해당 호출 키를 사용합니다.");out.add("[문열림] 현관 또는 공동현관과 연결된 상태에서 제품의 문열림 조작을 사용합니다. 외부기기는 도어 링커로 문과 연결되어 있어야 합니다.");if(kind.equals("Ip203apScreen")||kind.equals("Hmc7000Screen"))out.add("[표시] 음성·숫자 표시 전용 제품은 호출 영상을 표시하지 않습니다.");}
  else out.add("[설정] 현재 기기의 화면에서 이름·주소·연결 설정을 확인하고 저장합니다. 실제로 표시된 기능과 호환 연결 기기를 사용하세요.");
  out.add("[메시지] 통화 메시지 입력칸에 입력한 뒤 Enter 또는 전송으로 대화합니다. 사용법을 닫으면 원래 조작화면으로 돌아갑니다.");out.add("[이동·연결 범위] 기기 이동 도구로 위치를 조정합니다. 통화·사용 중이면 먼저 종료하세요. 연결과 통화는 같은 단지 안에서만 이루어집니다.");out.add("[사용법 닫기] 오른쪽 위 돌아가기 또는 Esc. 휠·방향키·PageUp/Down으로 읽습니다. 사용법을 보는 동안에도 호출과 기기 상태는 갱신됩니다.");return List.copyOf(out);
 }
 static void kvmText(KvmModel m,List<String> out){out.add("[모델] "+m.label()+" · "+m.bus+" · "+m.videoFormat());out.add("[착신·현관] 수화기 공간을 눌러 들고 착신에 응답합니다. 다시 거치하면 종료합니다. 현관 확인은 해당 모델의 현관 키를 사용합니다.");out.add("[본체 키] "+String.join(" / ",m.keys.stream().map(m::keyLabel).toList()));out.add("[문열림] 현관·공동현관 통화에서 문열림 기능으로 호출한 외부기기에 연결된 문을 엽니다. 도어 링커로 외부기기와 문을 연결하세요.");if(m.guard())out.add("[경비] 경비 키로 호환 경비실과 통화합니다. 경비실과 세대의 번호·방식을 맞추세요.");if(m.lobby())out.add("[공동현관] 호환 로비폰의 세대 호출에 응답합니다. 공동현관 기능은 같은 단지의 호환 방식에서 사용합니다.");if(m.apartment())out.add("[세대통화] 모델의 세대통화 기능에서 대상 세대 번호를 입력하여 호출합니다.");if(m.emergency())out.add("[비상] 비상 키로 경비실에 비상 알림을 보냅니다.");if(m.away())out.add("[외출] 외출 키로 재실·외출 상태를 전환합니다.");out.add("[설정·연결] 설정에서 주소를 지정하거나 해당 방식의 교환기에 등록합니다. 홈 링커로 세대현관을 연결하고, 교환기 방식은 호환 교환기와 연결합니다. 모델에 없는 키·기능은 사용할 수 없습니다.");}
}
