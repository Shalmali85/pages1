package org.dell.kube.pages;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HomeControllerTests {
<<<<<<< HEAD:src/test/java/org/dell/kube/pages/HomeControllerTests.java
    private final String message = "Hello Yellow Pages";
=======
    private final String message = "YellowPages";
>>>>>>> 998db8632575d234fcb23332867aab23fa81f871:src/test/java/org/dell/kube/pages/HomeControllerTests.java

    @Test
    public void itSaysYellowPagesHello() throws Exception {
        HomeController controller = new HomeController(message);

        assertThat(controller.getPage()).contains(message);
    }


}
