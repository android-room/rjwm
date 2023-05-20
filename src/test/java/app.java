import org.junit.jupiter.api.Test;

public class app {

    @Test
    public void test(){
        String name = "hello.jpg";
       name.substring(name.lastIndexOf("."));
        System.out.println(name.substring(name.lastIndexOf(".")));
    }
}
