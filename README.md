# Java-Bean-To-Ts-Interface
A InteliJ IDEA Plugin -  Convert java bean to typescript interface

Right click on a java bean and select "java bean to typescript interface", select the file save path, then a declaration file end with '.d.ts' will be saved in this folder
e.g.
Right click on TestRequest.java, then click 'java bean to typescript interface'
```java
public class TestRequest {
    /**
     * name list
     */
    @NotNull
    private String nameArray[];
    private List<String> names;

    private Boolean isRunning;

    private boolean isSuccess;

}
```
Result => TestRequest.d.ts
```typescript
export default interface TestRequest{
  /**
  * name list
  */
  nameArray: string[]

  names?: string[]

  isRunning?: boolean

  isSuccess?: boolean
}

```
