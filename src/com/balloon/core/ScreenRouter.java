// 이 클래스는 화면 전환을 담당하는 "라우터"야.
// 내부에 CardLayout을 쓰고, 화면을 id(문자열)로 등록/표시한다.
package com.balloon.core;

import javax.swing.*;  // JPanel, JComponent 같은 Swing 컴포넌트 사용
import java.awt.*;     // CardLayout 사용
import java.util.HashMap;
import java.util.Map;

/**
 * ScreenRouter
 * - CardLayout 기반의 화면 전환 컨트롤러
 * - 화면을 문자열 id로 등록하고, 필요할 때 show(id)로 전환
 * - 장점: 프레임 교체 없이 빠르고 깔끔하게 화면 전환 가능
 */
public class ScreenRouter {

    // 화면들이 붙을 "루트 컨테이너" (CardLayout을 적용한 패널)
    private final JPanel root;

    // 카드처럼 화면을 넘겨주는 레이아웃
    private final CardLayout layout;

    // 등록된 화면을 보관하는 레지스트리 (id -> 화면 컴포넌트)
    private final Map<String, JComponent> registry = new HashMap<>();

    // 생성자: CardLayout을 가진 JPanel을 만들고, root로 사용
    public ScreenRouter() {
        this.layout = new CardLayout();   // 카드 넘김 담당
        this.root = new JPanel(layout);   // layout이 적용된 컨테이너
    }

    // 외부에서 프레임의 contentPane으로 붙일 때 필요: frame.setContentPane(router.getRoot())
    public JPanel getRoot() {
        return root;
    }

    // 화면 등록: 특정 id로 JComponent(화면 패널)를 등록한다.
    // 예) router.register(ScreenId.START, new StartMenuUI(router));
    public void register(String id, JComponent screen) {
        registry.put(id, screen);  // Map에도 저장 (존재 확인/디버그에 유용)
        root.add(screen, id);      // CardLayout에 (컴포넌트, 이름)으로 추가
    }

    // 화면 전환: 해당 id가 등록되어 있어야 한다.
    // 없으면 실수 방지를 위해 예외를 던진다.
    public void show(String id) {
        if (!registry.containsKey(id)) {
            throw new IllegalArgumentException("Unregistered screen id: " + id);
        }
        layout.show(root, id);     // CardLayout에게 "이 이름의 카드 보여줘"라고 지시
    }
}
