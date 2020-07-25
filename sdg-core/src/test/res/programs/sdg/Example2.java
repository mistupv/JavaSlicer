package tfm.programs.sdg;

class InnerObject {
    int x;

    public InnerObject() {

    }

    public InnerObject(int x) {
        this.x = x;
    }
}

class CustomObject {
    InnerObject inner;

    public CustomObject() {

    }

    public CustomObject(InnerObject inner) {
       this.inner = inner;
    }
}


public class Example2 {


    public static void main(String[] args) {
        CustomObject customObject = new CustomObject(new InnerObject(1));
        modifyCustomObject(customObject);
        modifyInnerObject(customObject.inner);
    }

    public static void modifyCustomObject(CustomObject customObject) {
        customObject.inner = new InnerObject(2);
    }

    public static void modifyInnerObject(InnerObject innerObject) {
        innerObject.x = 5;
    }
}