package util;

import io.appium.java_client.MobileElement;
import org.openqa.selenium.By;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.List;


public class XPathUtil {
    public static org.slf4j.Logger log = LoggerFactory.getLogger(XPathUtil.class);
    public static XPath xpath = XPathFactory.newInstance().newXPath();
    private static DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

    private static Map<String, Map<String, Long>> monkeyClickedMap = new HashMap<>();
    private static Map<String, Long> clickedActivityMap = new HashMap<>();
    private static HashSet<String> set = new LinkedHashSet<>();
    private static DocumentBuilder builder;
    private static boolean stop = false;
    private static String appName;
    private static String appNameXpath;
    private static List<String> packageNameList;
    private static List<String> nodeBlackList;
    private static List<String> nodeWhiteList;
    private static List<String> nodeNameExcludeList;
    private static List<String> structureNodeNameExcludeList;
    private static List<String> pressBackPackageList;
    private static List<String> pressBackActivityList;
    private static List<String> backKeyTriggerList;
    private static List<String> xpathNotFoundElementList = new ArrayList<>();
    private static List<String> clickFailureElementList = new ArrayList<>();
    private static String clickXpath;
    private static String tabBarXpath;
    private static String firstLoginElemXpath;
    private static ArrayList<Map> loginElemList;
    private static Set<String> xpathBlackSet;
    private static Set<String> nodeXpathBlackSet;
    private static int scale;
    private static boolean ignoreCrash;
    private static boolean removedBounds = false;
    private static boolean swipeVertical = ConfigUtil.getBooleanValue(ConfigUtil.ENABLE_VERTICAL_SWIPE);
    private static long userLoginInterval;
    private static long userLoginCount = 0;

    //???back?????????????????? ??????app?????????
    private static int pressBackCount = 3;
    private static long clickCount = 0;
    private static long maxDepth = 0;
    private static String pic = null;
    private static String backKeyXpath = null;
    private static int deviceHeight;
    private static int deviceWidth;

    //Monkey related configuration
    private static Map<String,Long> monkeyEventRatioMap = new HashMap<>();
    private static Map<String,Long> monkeyEventSummaryRatioMap = new HashMap<>();
    private static List<Point> specialPointList = new ArrayList<>();
    private static List<Point> longPressPointList = new ArrayList<>();
    private static List<String> xpathItemList = new ArrayList<>();
    private static long runningTime;
    private static long testStartTime;// = System.currentTimeMillis();
    private static StringBuilder repoStep = new StringBuilder();

    public static HashSet<String> getSet() {
        return set;
    }
    public static Map<String, Long> getClickedActivityMap() {
        return clickedActivityMap;
    }
    public static Map<String, Map<String, Long>> getMonkeyClickedMap() {
        return monkeyClickedMap;
    }

    public static void showFailure(){
        log.info("Method: showFailure");

        int size = clickFailureElementList.size();
        log.info("clickFailureElementList??????size:"+size);
        if(size !=0){
            log.error("\n==============================Fail to click " + clickFailureElementList.size() + " elements \n" );

            for(String str : clickFailureElementList){
                log.info(str);
            }
        }else{
            log.info("Cool ! All the found elements are clicked");
        }


        size = xpathNotFoundElementList.size();
        log.info("xpathNotFoundElementList??????size:"+size);
        if(size != 0){
            log.error("\n\n==============================Fail to find " + size + " elements by xpath\n");

            for( String str: xpathNotFoundElementList){
                log.info(str);
            }
        }else{
            log.info("Congratulations ! All the xpath elements are found");
        }

        log.error("\n==============================");
    }

    private static void initMonkey(){
        log.info("Method: initMonkey");

        testStartTime = System.currentTimeMillis();
        //Init money ratio data
        for(String event : ConfigUtil.MONKEY_EVENT_RATION_LIST){
            long ratio = ConfigUtil.getLongValue(event);
            if(ratio > 0){
                monkeyEventRatioMap.put(event,ratio);
            }
        }

        //iOS?????????Home Key??????
        if(!Util.isAndroid()){
            monkeyEventRatioMap.remove(ConfigUtil.HOME_KEY_RATIO);
        }

        List<String> list = ConfigUtil.getListValue(ConfigUtil.MONKEY_SPECIAL_POINT_LIST);

        for(String str : list){
            str = str.trim();
            String[] value = str.split(",");
            int x = Integer.parseInt(value[0]);
            int y = Integer.parseInt(value[1]);

            specialPointList.add(new Point(x,y));
            log.info("Special point x: " + x + " y: " + y);
        }

        list = ConfigUtil.getListValue(ConfigUtil.LONG_PRESS_LIST);
        for(String str : list){
            str = str.trim();
            String[] value = str.split(",");
            int x = Integer.parseInt(value[0]);
            int y = Integer.parseInt(value[1]);

            longPressPointList.add(new Point(x,y));
            log.info("Long press point x: " + x + " y: " + y);
        }

        xpathItemList = ConfigUtil.getListValue(ConfigUtil.CLICK_ITEM_XPATH_LIST);

        log.info("Monkey running time is " + runningTime + " seconds");
        log.info("Monkey event list and ratio : \n" + monkeyEventRatioMap );
    }

    private static Set<String> getBlackKeyXpathSet(List<String> list){
        Set<String> set = new HashSet<>();

        for(String item : list){
            if(Util.isXpath(item)){
                set.add(item);
            }
        }

        log.info("Blacklist length is " + set.size());
        return set;
    }

    public static void initialize(String udid){
        log.info("Method: initialize");
        stop = false;
        runningTime = ConfigUtil.getLongValue(ConfigUtil.CRAWLER_RUNNING_TIME);
        testStartTime = System.currentTimeMillis();
        removedBounds = ConfigUtil.boundRemoved();
        userLoginInterval = ConfigUtil.getLongValue(ConfigUtil.USER_LOGIN_INTERVVAL);

        if(userLoginInterval <= 0){
            userLoginInterval = 1;
        }

        log.info("Running time is " + runningTime);

        if(runningTime <=0){
            runningTime = 24*6*7;
        }

        log.info("Crawler running time is " + runningTime);

        //?????????TabBar???????????? ????????? TabBar????????????

        //???XML???????????????
        if(Util.isAndroid(udid)){
            appNameXpath = "(//*[@package!=''])[1]";
            appName = ConfigUtil.getPackageName().trim();
        }else{
            appNameXpath = "//*[contains(@type,\"Application\")]";
            appName = ConfigUtil.getIOSBundleName().trim();
        }

        packageNameList = new ArrayList<>(Arrays.asList(appName));

        //??????????????????
        if(Util.isAndroid(udid)){
            packageNameList.addAll(ConfigUtil.getListValue(ConfigUtil.ANDROID_VALID_PACKAGE_LIST));
        }else{
            packageNameList.addAll(ConfigUtil.getListValue(ConfigUtil.IOS_VALID_BUNDLE_LIST));
        }

        //?????????
        nodeBlackList = new ArrayList<>();
        nodeBlackList.addAll(ConfigUtil.getListValue(ConfigUtil.ITEM_BLACKLIST));
        xpathBlackSet = getBlackKeyXpathSet(nodeBlackList);

        //?????????
        nodeWhiteList = new ArrayList<>();
        nodeWhiteList.addAll(ConfigUtil.getListValue(ConfigUtil.ITEM_WHITE_LIST));

        try{
            builder =  builderFactory.newDocumentBuilder();
        }catch (Exception e){
            log.error("!!!!!!!!!!document builder is null!!!!!!!!!!");
            e.printStackTrace();
        }

        //????????????????????????????????????Xpath
        if(Util.isAndroid(udid)){
            loginElemList = ConfigUtil.getListMapValue(ConfigUtil.ANDROID_LOGIN_ELEMENTS);

        }else{
            loginElemList = ConfigUtil.getListMapValue(ConfigUtil.IOS_LOGIN_ELEMENTS);
        }

        if(loginElemList.size() != 0){
            String key = (String)loginElemList.get(0).keySet().toArray()[0];
            firstLoginElemXpath = (( Map<String,String>)loginElemList.get(0).get(key)).get("XPATH");
            log.info("firstLoginElemXpath"+firstLoginElemXpath);
        }

        //??????????????????????????????Xpath
        StringBuilder clickBuilder;
        StringBuilder tabBuilder = null;

        //Android
        if(Util.isAndroid(udid)){
            //????????????tab bar???Xpath
            String androidBottomBarID =  ConfigUtil.getStringValue(ConfigUtil.ANDROID_BOTTOM_TAB_BAR_ID);
            clickBuilder = new StringBuilder("//*[");
            clickBuilder.append(ConfigUtil.getStringValue(ConfigUtil.ANDROID_CLICK_XPATH_HEADER));//@clickable="true"
            if(androidBottomBarID != null) {
                //tabBuilder = new StringBuilder("//*[@resource-id=\"" + androidBottomBarID + "\"]/descendant-or-self::*[@clickable=\"true\"]");
                tabBuilder = new StringBuilder("//*[" + androidBottomBarID +"]/descendant-or-self::*[@clickable=\"true\"]");//descendant-or-self  ?????????????????????????????????????????????????????????????????????????????????
                clickBuilder.append(" and not(ancestor-or-self::*[" +androidBottomBarID +"])");
            }

            //?????????????????????xPath
            for(String item: ConfigUtil.getListValue(ConfigUtil.ANDROID_EXCLUDE_TYPE)){
                clickBuilder.append(" and @class!=" + "\"" + item + "\"");
            }
        }else{
            //????????????tab bar???Xpath
            String iosBottomBarId =  ConfigUtil.getStringValue(ConfigUtil.IOS_BOTTOM_TAB_BAR_TYPE);
            clickBuilder = new StringBuilder("//*[");
            clickBuilder.append(ConfigUtil.getStringValue(ConfigUtil.IOS_CLICK_XPATH_HEADER));//@visible="true"
            if(iosBottomBarId != null){
                tabBuilder = new StringBuilder("//*[@visible=\"true\" and ancestor-or-self::" + iosBottomBarId +"]") ; //ancestor-or-self  ??????????????????????????????????????????????????????????????????????????????
                clickBuilder.append(" and not(ancestor-or-self::" + iosBottomBarId + ")");
            }

            //?????????????????????xPath
            for(String item : ConfigUtil.getListValue(ConfigUtil.IOS_EXCLUDE_BAR)){
                clickBuilder.append(" and not(ancestor-or-self::" + item + ")");
//                clickBuilder.append(" (descendant-or-self::" + item + ")");
            }

            for(String item :  ConfigUtil.getListValue(ConfigUtil.IOS_EXCLUDE_TYPE)){
                clickBuilder.append(" and @type!=\"" + item + "\"");
            }
        }

        clickBuilder.append("]");
        clickXpath = clickBuilder.toString();
        if(tabBuilder != null) {
            tabBarXpath = tabBuilder.toString();
            log.info("tab bar xpath: " + tabBarXpath);
        }else {
            log.info("tab bar has no xpath");
        }

        log.info("clickable elements xpath: " + clickXpath);

        //Get screen scale
        scale = Driver.getScreenScale();
        log.info("scale:"+scale);
        log.info("??????Driver?????????pageSourceDriver.driver.getPageSource3():"+Driver.driver.getPageSource());
        log.info("?????????????????????Driver.getPageSource3():"+Driver.getPageSource());
        deviceHeight = Driver.getDeviceHeight();
        deviceWidth = Driver.getDeviceWidth();
        log.info("??????Driver?????????pageSourceDriver.driver.getPageSource4():"+Driver.driver.getPageSource());
        log.info("?????????????????????Driver.getPageSource4():"+Driver.getPageSource());
        ignoreCrash = ConfigUtil.getBooleanValue(ConfigUtil.IGNORE_CRASH);
        //xpath?????????????????????, ??????android  ????????????
        nodeNameExcludeList = ConfigUtil.getListValue(ConfigUtil.NODE_NAME_EXCLUDE_LIST);
        //xpath?????????????????????, android???iOS  ????????????
        structureNodeNameExcludeList = ConfigUtil.getListValue(ConfigUtil.STRUCTURE_NODE_NAME_EXCLUDE_LIST);
        maxDepth = ConfigUtil.getDepth();
        pressBackPackageList = ConfigUtil.getListValue(ConfigUtil.PRESS_BACK_PACKAGE_LIST);
        if(Driver.isMicroProgramme(appName)){
            pressBackPackageList.remove("com.tencent.mm");
        }

        pressBackActivityList = ConfigUtil.getListValue(ConfigUtil.PRESS_BACK_ACTIVITY_LIST);

        if(Util.isAndroid()){
            backKeyXpath = ConfigUtil.getStringValue(ConfigUtil.ANDROID_BACK_KEY);
        }else{
            backKeyXpath = ConfigUtil.getStringValue(ConfigUtil.IOS_BACK_KEY);
        }

        backKeyTriggerList = ConfigUtil.getListValue(ConfigUtil.PRESS_BACK_TEXT_LIST);
    }

    public static PackageStatus isValidPackageName(String packageName){
        return isValidPackageName(packageName,true);

    }

    //?????????????????????false??? ????????????crash??????????????????app
    //????????????ignoreCrash=true, stop??????????????????false, ????????????app
    //????????????ignoreCrash=fasle, stop????????????true??????????????????app
    public static PackageStatus isValidPackageName(String packageName, boolean restart){
        log.info("Method: isValidPackageName");
        PackageStatus isValid = PackageStatus.CRASHED;

        //??????????????????????????????
        for(String name : packageNameList){
            if(null != packageName && packageName.contains(name)){
                isValid = PackageStatus.VALID;
                break;
            }
        }

        stop = false;

        //?????????????????????app??????Crash(?????????VALID_PACKAGE_LIST???)
        log.info("isValidPackageName???isValid:"+isValid);

        if(isValid != PackageStatus.VALID){
            log.error("----------!!!!!!!!!!not valid package name : " + packageName);

            Driver.takeScreenShot();

            String processName = appName;
            if(!Util.isAndroid()){
                processName = ConfigUtil.getStringValue(ConfigUtil.IOS_IPA_NAME);
            }

            //?????????crash??? ???????????????????????????app ???isValid??????true,??????app??????
            if(Util.isProcessExist(ConfigUtil.getUdid(),processName)){
                isValid = PackageStatus.APP_RESTART;
                if(pressBackPackageList.contains(packageName)){
                    isValid = PackageStatus.PRESS_BACK;
                    log.info("Package name :" + packageName + "is in pressBackList, so press back key");
                    Driver.takesScreenShotAndPressBack(repoStep);
                }
            }else{
                //??????crash????????????ignoreCrash???true,?????????app????????????
                stop = !ignoreCrash;
                isValid = PackageStatus.CRASHED;
                Util.renameAndCopyCrashFile(pic);
                Driver.takeScreenShot();
                log.error("===================app crashed!!!");
            }

            if((isValid == PackageStatus.APP_RESTART || !stop) && restart) {
                //relaunch??? ??????CurrentXml ??????getPageSource
                Driver.appRelaunch(repoStep);
            }
        }

        log.info(packageName + ": isValid=" + isValid);

        return isValid;
    }

    public static String clickElement(MobileElement elem, String xml){
        log.info("Method: clickElement");

        String page = xml;

        try {
            if(Util.isAndroid()){
                String activityName = Driver.getCurrentActivity();
                Long clickCount = clickedActivityMap.get(activityName);

                if(clickCount == null){
                    clickCount = 1L;
                }else{
                    clickCount ++;
                }

                clickedActivityMap.put(activityName,clickCount);
                log.info("clickedActivityMap = " + clickedActivityMap.toString());
            }

            //Sometimes, UIA2 will throw StaleObjectException Exception here
            int x = elem.getCenter().getX();
            int y = elem.getCenter().getY();

            pic = PictureUtil.takeAndModifyScreenShot(x*scale,y*scale);

            //String appName;
            clickCount++;
            repoStep.append("CLICK : " + x + "," + y + "\n");

            clickFailureElementList.add(elem.toString());
            elem.click();
            clickFailureElementList.remove(elem.toString());

            log.info("------------------------CLICK " + clickCount + "  X: " + x + " Y: " + y +" --------------------------");

            //?????????UI????????? ????????????
            Driver.sleep(1);
            page = Driver.getPageSource();
            String appName = getAppName(page);

            PackageStatus status = isValidPackageName(appName,false);

            if(PackageStatus.VALID != status){
                page = Driver.getPageSource();
            }

            if(clickCount >= ConfigUtil.getClickCount()){
                stop = true;
            }

            String temp ="";

            try{
                String elemStr = elem.toString();
                List<String> inputClassList = ConfigUtil.getListValue(ConfigUtil.INPUT_CLASS_LIST);
                List<String> inputTextList = ConfigUtil.getListValue(ConfigUtil.INPUT_TEXT_LIST);
                int size = inputClassList.size();

                for(String elemClass : inputClassList){
                    temp = elemClass;
                    if(elemStr.contains(elemClass)){
                        int index = Util.internalNextInt(0,size);
                        String text = inputTextList.get(index);
                        elem.setValue(text );
                        repoStep.append("INPUT :" + elem.toString() + " ; " + text + "\n");
                        log.info("Element " + temp + " set text : " + text);
                        break;
                    }
                }
            }catch (Exception e){
                log.error("fail to set text for class : " + temp);
            }

        }catch (Exception e){
            e.printStackTrace();
            log.error("\n!!!!!!Fail to click elem : " + elem);

            if(!Util.isAndroid()){
                //iOS??? app??????crash??? ????????????getPageSource ??????WDA???????????????
                log.info("ConfigUtil.getUdid():"+ConfigUtil.getUdid());
                log.info("ConfigUtil.getStringValue(ConfigUtil.IOS_IPA_NAME):"+ConfigUtil.getStringValue(ConfigUtil.IOS_IPA_NAME));
                if(!Util.isProcessExist(ConfigUtil.getUdid(),ConfigUtil.getStringValue(ConfigUtil.IOS_IPA_NAME))){
                    Util.renameAndCopyCrashFile(pic);
                    stop = !ignoreCrash;
                    page = xml;

                    if(ignoreCrash){
                        Driver.appRelaunch();
                        page = Driver.getPageSource();
                    }
                }
            }else{
                page = Driver.getPageSource();
            }
        }

        return  page;
    }

    private static Set<String> getBlackNodeXpathSet(Document document) {
        String xpathStr = "";
        Set<String> nodeSet =  new HashSet<>();

        try {
            for (String item : xpathBlackSet) {
                xpathStr = item;
                NodeList nodes = (NodeList) xpath.evaluate(item, document, XPathConstants.NODESET);
                int length = nodes.getLength();

                while(-- length >= 0){
                    Node tmpNode = nodes.item(length);
                    String nodeXpath = getNodeXpath(tmpNode);

                    if(nodeXpath != null){
                        nodeSet.add(nodeXpath);
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            log.error("Fail to deal with black key xpath " + xpathStr);
        }

        log.info("Black xpath set length is : "  + nodeSet.size());
        return nodeSet;
    }

    public static String getNodesFromFile(String xml, long currentDepth) throws Exception{
        log.info("Method: getNodesFromFile");
        log.info("Driver.driver:"+Driver.driver);
        log.info("xml:"+xml);
//        log.info("Context: " + Driver.driver.getContextHandles().toString());

        if (ConfigUtil.isAutoLoginEnabled()) {
            try {
                log.info("userLoginCount:"+userLoginCount);
                log.info("userLoginInterval:"+userLoginInterval);
                log.info("0 == userLoginCount % userLoginInterval:"+(0 == userLoginCount % userLoginInterval));
                if (0 == userLoginCount % userLoginInterval) {   //?????????????????????ui???????????????????????????????????????userLoginCount=0???userLoginInterval=5
                    log.info("Processing login operation");
                    xml = userLogin(xml);
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.error("Fail to log in!");
            }

            userLoginCount++;
        }

        //??????????????????
        long endTime = System.currentTimeMillis();

        if((endTime - testStartTime) > ( runningTime * 60 * 1000)) {
            log.info("?????????" + (endTime - testStartTime)/60/1000 + "???????????????????????????");
            stop = true;
            return xml;
        }

        Document document = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        log.info("clickXpath:"+clickXpath);
        log.info("document:"+document);
        log.info("XPathConstants.NODESET:"+XPathConstants.NODESET);
        NodeList nodes = (NodeList) xpath.evaluate(clickXpath, document, XPathConstants.NODESET);

        log.info(String.valueOf("UI nodes length : " + nodes.getLength()));

        int length = nodes.getLength();

        String previousPageStructure = Driver.getPageStructure(xml,clickXpath);
        String afterPageStructure = previousPageStructure;
        String currentXML = xml;

        //??????stop???true???????????????
        if(stop){
            log.info("-----stop=true, Fast exit");
            return currentXML;
        }

        log.info("Back key list size is " + backKeyTriggerList.size());

        //?????????????????????Back Key
        if( backKeyTriggerList.size() > 0 ){
            for(String key : backKeyTriggerList){
                if (currentXML.contains(key)){
                    log.info("Back key trigger text: " + key + " is found, press back key");
                    Driver.takeScreenShot();
                    Driver.pressBack();
                    currentXML = Driver.getPageSource();
                    return currentXML;
                }
            }
        }

        if(pressBackActivityList.size() > 0){
            String currentActivity = Driver.getCurrentActivity();
            //log.error("Current activity " + currentActivity);

            for(String key : pressBackActivityList){
                if (currentActivity.contains(key)){
                    log.info("Back key trigger activity: " + key + " is found, press back key");
                    Driver.takeScreenShot();
                    Driver.pressBack();
                    currentXML = Driver.getPageSource();
                    return currentXML;
                }
            }
        }

        //???????????????
        // 1.???????????????????????????????????????????????????
        // 2.??????Depth,????????????????????????
        // 3.???????????????????????????,??????????????????TabBar,?????????TabBar
        String packageName = getAppName(currentXML);

        if (packageName.equals("com.tencent.mm") || packageName.equals("com.tencent.xin")){
            if(currentXML.contains("??????????????????")){
                log.info("?????????????????????????????????????????????????????????????????????");
                stop = true;

                return currentXML;
            }
        }

        log.info("++++++++++++++++++ Activity Name : " + Driver.getCurrentActivity() +"+++++++++++++++++++++++");

        //1.????????????UI????????????????????????????????????????????????????????????????????????stop??????, ??????????????????-??????????????????app?--
        if(PackageStatus.VALID != isValidPackageName(packageName,true)){
            log.info("=====================package: "+ packageName + " is invalid, return ....==============================");
            currentXML = Driver.getPageSource();
            return currentXML;
        }

        // 2.??????Depth,????????????????????????
        currentDepth++;
        log.info("------Depth: " + currentDepth);
        if(currentDepth > maxDepth){
            stop = true;
            log.info("Return because exceed max depth: " + maxDepth);
            //Driver.pressBack(repoStep);
            currentXML = Driver.getPageSource();
            return currentXML;
        }

        log.info("node length is " + length);

        //3.?????????????????????????????????????????????, ????????????????????????????????????TabBar????????????????????????return?????????tabBar???????????????
        if(length == 0){
            log.error("========!!!!!!!No UI node found in current page!!!!! Begin to find tab bar element... =====");
        }

        showTabBarElement(currentXML,tabBarXpath);

        //???????????????xpath
        nodeXpathBlackSet =  getBlackNodeXpathSet(document);
        int blackNodeXpathSize = nodeXpathBlackSet.size();
        log.info("black node Xpath size is " + blackNodeXpathSize);

        //??????UI??????Node??????
        while(--length >= 0 && !stop){
            log.info("Element index is : " + length);

            Node tmpNode = nodes.item(length);
            String nodeXpath = getNodeXpath(tmpNode);

            if(nodeXpath == null){
                log.error("Null nodeXpath , continue.");
                continue;
            }

            if(blackNodeXpathSize != 0){
                if(nodeXpathBlackSet.contains(nodeXpath)){
                    log.info("Ignore black xpath item : " + nodeXpath);
                    continue;
                }
            }

            //Comment this if not in test mode
            //nodeXpath = showNodes(currentXML,nodeXpath);

            //?????????????????????????????????
            if(set.add(nodeXpath)){

                //???????????????
                for (String item : nodeWhiteList) {
                    if (nodeXpath.contains(item.trim())) {
                        log.info("-------remove " + item + " since it is in white list : " + nodeWhiteList);
                        set.remove(nodeXpath);
                    }
                }

                MobileElement elem = Driver.findElementWithoutException(By.xpath(nodeXpath));

                if(null == elem){
                    //??????????????????????????????????????????
                    xpathNotFoundElementList.add(nodeXpath);
                    log.info("---------Node not found in current UI!!!!!!! Stop current iteration.-----------" );
                    log.info("??????Driver?????????pageSourceDriver.driver.getPageSource1():"+Driver.driver.getPageSource());
                    break;
                }

                currentXML = clickElement(elem,currentXML);
                afterPageStructure = Driver.getPageStructure(currentXML,clickXpath);

                //?????????????????????????????????
                if(!stop && !isSamePage(previousPageStructure,afterPageStructure)) {
                    log.info("========================================New Child UI================================");

                    //?????????UI??? ???????????????????????????
                    packageName = getAppName(currentXML);

                    if(PackageStatus.VALID != isValidPackageName(packageName)){
                        currentXML = Driver.getPageSource();
                        afterPageStructure = Driver.getPageStructure(currentXML,clickXpath);
                        break;
                    }

                    //?????????UI
                    getNodesFromFile(currentXML,currentDepth);

                    //????????????????????????
                    // 1.??????????????????
                    // 2.stop?????????????????????????????????true

                    //??????????????????????????????????????? ??????????????????
                    if(stop){
                        break;
                    }

                    //??????UI????????????????????????
                    currentXML = Driver.getPageSource();
                    packageName = getAppName(currentXML);
                    if(PackageStatus.VALID != isValidPackageName(packageName,false)){
                        break;
                    }

                    //????????????????????????????????????????????????????????????
                    afterPageStructure = Driver.getPageStructure(currentXML,clickXpath);

                    if(isSamePage(previousPageStructure,afterPageStructure)){
                        log.info("Parent page stay the same after returning from child page");
                        //?????????????????? ????????????
                    }else{
                        log.info("Parent page changed after returning from child page");
                        //????????????????????? ???????????????????????? ????????????????????????
                        break;
                    }
                }else{
                    log.info("========================================Same UI");
                }
            }else {
                //?????????????????????
                log.info("---existed--- " + nodeXpath + "\n");
            }
        }

        log.info("\n\n\n!!!!!!!!!!!!!Done!!!!!!!!!!!!!!! stop: " + stop +"\n\n\n");

        //currentDepth --;

        //??????????????????????????????
        if(stop){
            log.info("\n\n\nPackage name changed: "+ packageName + " set stop to true and return!!!!!!\n\n\n");
            return currentXML;
        }

        log.info("node length after while is " + length);
        boolean shouldPressBack = true;

        //???????????????????????????,???UI????????????????????????????????????
        if(length < 0 && isSamePage(previousPageStructure,afterPageStructure)) {

            //?????????????????????
            if(swipeVertical){
                log.info("Swipe vertical is enabled.");

                Driver.swipeVertical(false,repoStep);
                currentXML = Driver.getPageSource();

                previousPageStructure = afterPageStructure;
                afterPageStructure = Driver.getPageStructure(currentXML,clickXpath);

                //????????????????????????????????????
                if(!isSamePage(previousPageStructure,afterPageStructure)){
                    log.info("Page changes after vertical swipe");
                    shouldPressBack = false;
                    currentXML = getNodesFromFile(currentXML,currentDepth);
                }else{
                    log.info("No change found after vertical swipe");
                    //shouldPressBack = true;
                }
            }

            log.info("all the non tab bar nodes in current UI are iterated.");

            //??????????????????press back
            if(shouldPressBack){
                if(backKeyXpath !=null){
                    log.info("Finding back key ");

                    MobileElement elem = Driver.findElementWithoutException(By.xpath(backKeyXpath));

                    if(elem!=null){
                        log.info("Back key found , clicking back key...");
                        currentXML = clickElement(elem,currentXML);
                        return currentXML;
                    }else{
                        log.info("Back key is not found");
                    }
                }

                //???????????????TabBar, ?????????TabBar ??????TabBar???????????? ????????????new page UI XML
                currentXML = clickTabBarElement(currentXML, tabBarXpath);

                if(null != currentXML){
                    log.info("========================================TabBar found, click TabBar and iterate new UI");
                    getNodesFromFile(currentXML,currentDepth);
                }else{
                    //log.info("========================================No TabBar element found");
                    //???TabBar???TabBar??????????????????????????????????????????back key
                    Driver.pressBackAndTakesScreenShot();
                    log.info("========================================press back");

                    currentXML = Driver.getPageSource();
                    packageName = getAppName(currentXML);

                    if(pressBackCount<=0){
                        log.info("Press count is 0, so stop testing");
                        stop = true;
                    }

                    //???back Key?????? ????????????Home Screen ????????????app
                    if(pressBackCount > 0 && isValidPackageName(packageName,false) != PackageStatus.VALID ){
                        log.info("Returned to Home Screen,due to press back key, so restart app... pressBackCount is :" + pressBackCount);
                        Driver.appRelaunch(repoStep);

                        pressBackCount--;
                        currentXML =  Driver.getPageSource();
                        getNodesFromFile(currentXML,currentDepth);
                    }
                }
            }
        }else{
            //?????????????????????????????????????????? ???break????????? ???????????????????????????
            log.info("========================================Stop current UI page testing. Due to UI change");
            log.info("??????Driver?????????pageSourceDriver.driver.getPageSource2():"+Driver.driver.getPageSource());
            currentXML = Driver.getPageSource();
            getNodesFromFile(currentXML,currentDepth);

        }

        log.info("========================================Complete iterating current UI with following elements: ");
        //log.info( "\n\n\n" + previousPageStructure + "\n\n\n");
        log.info("depth before return is " + currentDepth);
        return currentXML;
    }


    private static NodeList getNodeListByXpath(String xml, String xpathExpr) throws Exception{
        Document document = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        return (NodeList) xpath.evaluate(xpathExpr, document, XPathConstants.NODESET);
    }


    private static String getAppName(String xml){
        log.info("Method: getAPPName");
        String name = null;
        NodeList nodeList = null;

        try{
            nodeList = getNodeListByXpath(xml,appNameXpath);
        }catch (Exception e){
            log.info("xml:"+xml);
            e.printStackTrace();
        }

        if(null == nodeList || nodeList.getLength() == 0){
           log.error("null app name get from xml file");

           if(!Util.isAndroid()){
               //TODO:??????iOS?????????XCUIAPPLICATION
               name = ConfigUtil.getStringValue("IOS_BUNDLE_NAME");
           }

           return name;
        }

        Node node = nodeList.item(0);

        String key = "package";

        //ios???app?????????
        if(!Util.isAndroid()){
            key = "name";
        }

        name = node.getAttributes().getNamedItem(key).getNodeValue();

        //iOS?????? alert
        if(!Util.isAndroid()){
            log.info("Check if there is any alert in iOS");
            if(xml.contains("XCUIElementTypeAlert") && "".equals(name.trim())){
                log.info("Alert found! Set package name to app name");
                name = ConfigUtil.getIOSBundleName();
            }
        }

        log.info("-------AppName---------" + name);
        return name;
    }

    @SuppressWarnings("unchecked")
    public static String userLogin(String xml){
        log.info("Method: userLogin");

        if(firstLoginElemXpath == null){
            return xml;
        }
        try {
            Document document = builder.parse(new ByteArrayInputStream(xml.getBytes()));
            NodeList nodes = (NodeList) xpath.evaluate(firstLoginElemXpath, document, XPathConstants.NODESET);

            log.info("firstLoginElemXpath : " + firstLoginElemXpath + " nodes.getLength()  : " + nodes.getLength());

            //?????????????????????
            if (true){
            //if(nodes.getLength() == 1){
                for(Map map : loginElemList){
                    String key = (String) map.keySet().toArray()[0];
                    map = ((Map<String,String>) map.get(key));
                    String xpath = map.get(ConfigUtil.XPATH).toString();
                    String action =  map.get(ConfigUtil.ACTION).toString();
                    String value =  String.valueOf(map.get(ConfigUtil.VALUE));

                    try {
                        log.info("userLogin Element. xpath:"+xpath+";action:"+action+";value:"+value);
                        triggerElementAction(xpath, action, value);
                    }catch (Exception e){
                        log.error("userLogin Element is not found. "+ xpath);
                    }
                }

                //????????????????????????
                Driver.sleep(10);
                xml = Driver.getPageSource();
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return xml;
    }

    private static void triggerElementAction(String xpath, String action, Object value){
        MobileElement element = Driver.findElement(By.xpath(xpath));

        log.info("Trigger element : " + xpath + " action : " + action + " value : " + value );

        switch (action.toCharArray()[0]){
            case 'c'://click
                element.click();
                if(value != null){
                    Driver.sleep(Integer.parseInt(value.toString()));
                }
                break;
            case 'i'://input
                element.clear();
                element.setValue(value.toString());
                break;
            case 'd'://drag
                List<String> pointsList = Arrays.asList(value.toString().split(","));
                Driver.drag(pointsList);
            default:
                break;
        }
    }

    protected static boolean isSamePage(String pre,String after){
        log.info("Method: isSamePage");
        boolean ret = pre.equals(after);

        //??????????????????, ???????????????????????????stop=false????????????
        if(!ret && !stop) {
            //TODO:Remove comment
            //Driver.takeScreenShot();
            log.info("++++++++++++++++++ Activity Name  :  " + Driver.getCurrentActivity() + "+++++++++++++++++++++++");
        }

        return ret;
    }

    private static String getNodeXpath(Node node){
        return getNodeXpath(node,false);
    }

    public static String getNodeXpath(Node node, boolean structureOnly){
        int length = node.getAttributes().getLength();
        StringBuilder nodeXpath = new StringBuilder("//" + node.getNodeName() + "[");
        //xpath?????????????????????, ??????android  ????????????
        //final List<String> nodeNameExcludeList = new ArrayList<>(Arrays.asList("selected","instance","checked","naf","content-desc"));
        //xpath?????????????????????, android???iOS  ????????????
        //inal List<String> structureNodeNameExcludeList = new ArrayList<>(Arrays.asList("value","lable","name" ,"text"));

        String bounds = "[" + deviceWidth + ","  + deviceHeight + "]";

        while(--length >= 0){

            Node tmpNode = node.getAttributes().item(length);

            String nodeName = tmpNode.getNodeName();
            String nodeValue = tmpNode.getNodeValue();

            if(nodeValue.length() == 0){
                continue;
            }

            //TODO: ???bounds?????????width??? Xpath?????????????????????????????????"??????"?????????????????? ???Xpath?????????bounds??????  ???????????????????????????
            if(removedBounds && nodeValue.contains(bounds)){
                log.info("Remove bounds,since its value is "+ nodeValue +  " same as screen height and width");
                continue;
            }

            //???????????????
            for(Object item : nodeBlackList){
                log.info("nodeValue:"+nodeValue);
                log.info("String.valueOf(item):"+String.valueOf(item));
                if(nodeValue.contains(String.valueOf(item))){
                    log.info("-----------------black list item---" + nodeValue);
                    return null;
                }
            }

            //?????????????????????node??????
            if(nodeNameExcludeList.contains(nodeName.toLowerCase())){
                continue;
            }

            //UI?????? ????????? "value","lable","name"???node??????
            if(structureOnly && structureNodeNameExcludeList.contains(nodeName.toLowerCase())){
                continue;
            }

            //???????????????????????????????????????width???height, Android only
            if(Util.isAndroid() && nodeName.equals("bounds")){
                //String value="[1080,468][2160,885]";
                String value = nodeValue;
                value =value.replace("][",",");//"[1080,468,2160,885]";

                int index = value.indexOf(",");
                int startX = Integer.valueOf(value.substring(1,index));

                int indexNext = value.indexOf(",",index+1);
                int startY = Integer.valueOf(value.substring(index+1,indexNext));

                index = value.indexOf(",",indexNext + 1);
                int endX = Integer.valueOf(value.substring(indexNext +1,index));
                int endY = Integer.valueOf(value.substring(index +1,value.length()-1));

                //log.info(String.valueOf(startX));log.info(String.valueOf(startY));log.info(String.valueOf(endX));log.info(String.valueOf(endY));

                //??????????????????????????????
                if(startX <0 || startY <0 || endX <0 || endY <0){
                    log.info("<0 ----Removed-----" +nodeValue);
                    return null;
                }

                if(startX > deviceWidth || endX > deviceWidth ||startY > deviceHeight || endY > deviceHeight){
                    log.info(">max ----Removed-----" +nodeValue);
                    return null;
                }

                //??????????????? ???webview??? ?????????????????????????????????????????????
                //TODO:do it for ios, ??????????????? ???webview??? ?????????????????????????????????????????????
                int tolerance = 5;
                if(Math.abs(endX - startX) < tolerance || Math.abs(endY - startY) < tolerance){
                    if (ConfigUtil.isShowDomXML() ){
                        log.info("Removed due to exceed tolerance : " + tolerance + " StartX: " + String.valueOf(startX) + " StartY: " + String.valueOf(startY) + " EndX: " + String.valueOf(endX) + " EndY: " + String.valueOf(endY));
                    }
                    return null;
                }
            }

            //?????????????????????????????? iOS only  ??????????????????????????????????????????????????? visible=false ??????????????????????????????
            if(!Util.isAndroid() && nodeName.equals("x")){
                int x = Integer.valueOf(nodeValue);
                if(x > deviceWidth){
                    log.info(x +">max ----Removed-----");
                    return null;
                }
            }

            if(!Util.isAndroid() && nodeName.equals("y")){
                int y = Integer.valueOf(nodeValue);
                if( y > deviceHeight){
                    log.info(y + ">max ----Removed-----");
                    return null;
                }
            }

            nodeXpath.append("@");
            nodeXpath.append(nodeName);
            nodeXpath.append("=\"");
            nodeXpath.append(nodeValue);
            nodeXpath.append("\"");

            if (length > 0) {
                nodeXpath.append(" and ");
            }
        }

        nodeXpath.append("]");

        //log.info(nodeXpath.toString());
        return nodeXpath.toString().replace(" and ]","]");
    }

    public static String clickTabBarElement(String xml, String expression) throws Exception{
        log.info("Method clickTabBarElement");

        String newXml = null;

        if(expression == null){
            return newXml;
        }

        log.info("Tab bar xpath expression: " + expression);

        Document document = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        NodeList nodes = (NodeList) xpath.evaluate(expression, document, XPathConstants.NODESET);

        log.info(String.valueOf("TabBar nodes length : " + nodes.getLength()));

        int length = nodes.getLength();

        if(length == 0) {
            log.info("No TabBar elements found!");
            return newXml;
        }
        log.info("length:"+length);
        while(length > 0 ){
            length --;
            Node tmpNode = nodes.item(length);
            String nodeXpath = getNodeXpath(tmpNode);

            if(nodeXpath==null){
                continue;
            }

            //?????????????????????????????????
            if(set.add(nodeXpath)){
                MobileElement elem = Driver.findElementWithoutException(By.xpath(nodeXpath));

                if(null == elem){
                    log.info("---------TabBar Element Elem not found!!!!!!! Stop current iteration.-----------" );
                    break;
                }

                log.info("------------------------CLICK TabBar Element-------------------------- \n" + nodeXpath);
                newXml = clickElement(elem,xml);

                //TODO: Remove this
                PictureUtil.takeAndModifyScreenShot(400,400,600,"click-tabbar");
                break;
            }
        }

        if(length == 0){
            log.info("All the tab bar elements are clicked!");
        }

        return newXml;
    }

    public static String showTabBarElement(String xml, String expression) throws Exception{
        log.info("Method showTabBarElement");

        String newXml = null;

        if(expression == null){
            return newXml;
        }

        log.info("Tab bar xpath expression: " + expression);

        Document document = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        NodeList nodes = (NodeList) xpath.evaluate(expression, document, XPathConstants.NODESET);

        log.info(String.valueOf("tab bar nodes length : " + nodes.getLength()));

        int length = nodes.getLength();

        if(length == 0){
            log.info("No tab bar elements found!");
            return newXml;
        }

        while(length > 0 ) {
            length--;
            Node tmpNode = nodes.item(length);
            String nodeXpath = getNodeXpath(tmpNode);

            if (nodeXpath == null) {
                continue;
            }

            log.info("Tab " + nodeXpath);
        }

        return newXml;
    }

    public static long getClickCount() {
        log.info("Method: getClickCount");
        return clickCount;
    }

    private static ArrayList<String> initEventMap(){
        log.info("Method: initEventMap");

        ArrayList<String> array = new ArrayList<>();

        for(String event : monkeyEventRatioMap.keySet()){
            array.add(event);
            monkeyEventSummaryRatioMap.put(event,0L);
        }

        log.info(array.toString());
        return array;
    }



    public static void monkey(String pageSource){
        log.info("Method: monkey");

        userLogin(pageSource);
        initMonkey();

        boolean isLandscape = Driver.isLandscape();

        int GAP_X = 80;
        int GAP_Y = 80;
        int x,y;
        int index;

        if(!Util.isAndroid()){
            GAP_X = 50;
            GAP_Y = 50;
        }

        //???????????????????????????
        int actualWidth = deviceWidth - GAP_X;
        int actualHeight = deviceHeight - GAP_Y;
        log.info("Actual width : "+deviceWidth*scale + " Actual height : " + deviceHeight*scale);

        int centerX = deviceWidth/2;
        int centerY = deviceHeight/2;

        if(isLandscape){
            centerX = deviceHeight/2;
            centerY = deviceWidth/2;
        }

        longPressPointList.add(new Point(centerX, centerY));
        log.info("Center X " + centerX + " Center Y " + centerY);

        Driver.sleep(5);

        ArrayList<String> ratioList = initEventMap();

        if(ratioList.size()==0){
            log.error("Available list size is 0");
            return;
        }

        log.info("Monkey running time is (minutes): " + runningTime);

        int specialPointSize = specialPointList.size();
        int spIndex = 0;

        int longPressPointSize = longPressPointList.size();
        int lpIndex = 0;

        int xpathListSize = xpathItemList.size();
        int xpIndex = 0;

        while (true) {
            long endTime = System.currentTimeMillis();

            if((endTime - testStartTime) > ( runningTime * 60 * 1000)) {
                log.info("?????????" + (endTime - testStartTime)/60/1000 + "???????????????????????????");
                break;
            }

            log.info("Available event list : " + ratioList);
            x = Util.internalNextInt(GAP_X, actualWidth);
            y = Util.internalNextInt(GAP_Y, actualHeight);

            if(isLandscape){
                //Exchange X,Y in landscape mode
                x=x+y;
                y=x-y;
                x=x-y;
            }

            int length = ratioList.size() ;

            if(length <= 0){
                ratioList = initEventMap();
                length = ratioList.size();
                log.info("----------------------------------------------Reinitialize ration-----------------");
            }

            index = Util.internalNextInt(0, length);
            String event = ratioList.get(index);

            log.info("index is "  + index );

            Long count = monkeyEventSummaryRatioMap.get(event);

            if(count >= monkeyEventRatioMap.get(event)){
                log.info("---------Remove event " + event);
                ratioList.remove(index);
                continue;
            }else{
                count ++;
                monkeyEventSummaryRatioMap.put(event,count);
                log.info("\n\n================Event " + event + " count is " + count + " ====================\n");
            }

            //iOS has no concept of activity
            if(Util.isAndroid()){
                String eventType = event.replace("_RATIO","");
                String currentActivityName = Driver.getCurrentActivity();
                Map<String, Long> eventCountMap = monkeyClickedMap.get(currentActivityName);

                Long eventCount = 1L;

                if(eventCountMap == null){
                    eventCountMap = new HashMap<>();
                }else{
                    eventCount = eventCountMap.get(eventType);

                    if(eventCount != null){
                        eventCount ++;
                    }else{
                        eventCount = 1L;
                    }
                }

                log.info("Event count " + eventCount );

                eventCountMap.put(eventType,eventCount);
                monkeyClickedMap.put(currentActivityName,eventCountMap);

                log.info("Current activity : " + currentActivityName);
                log.info(monkeyClickedMap.toString());
            }

            int endX= Util.internalNextInt(GAP_X, actualWidth);
            int endY= Util.internalNextInt(GAP_Y, actualHeight);

            List<Point> pointList = new ArrayList<>();
            pointList.add(new Point(x*scale,y*scale));
            pointList.add(new Point(endX*scale,endY*scale));

            try{
                switch (event){
                    case ConfigUtil.CLICK_RATIO:
                        PictureUtil.takeAndModifyScreenShotAsyn(x * scale, y * scale);
                        Driver.clickByCoordinate(x, y);
                        break;
                    case ConfigUtil.SWIPE_RATIO:
                        Driver.takeScreenShot();

                        if(isLandscape){
                            //Exchange X,Y in landscape mode
                            endX=endX+endY;
                            endY=endX-endY;
                            endX=endX-endY;
                        }
                        Driver.swipe(x, y, endX, endY);//?????????????????????
                        break;
                    case ConfigUtil.RESTART_APP_RATIO:
                        Driver.appRelaunch();
                        break;
                    case ConfigUtil.HOME_KEY_RATIO:
                        Driver.pressHomeKey();
                        break;
                    case ConfigUtil.CLICK_SPECIAL_POINT_RATIO:
                        log.info("Special point list size : " + specialPointSize);
                        log.info("Special point list  : " + specialPointList);
                        log.info("spIndex  is " + spIndex);

                        spIndex = spIndex % specialPointSize;
                        //int random = Util.internalNextInt(0,size);
                        Point point = specialPointList.get(spIndex++);

                        PictureUtil.takeAndModifyScreenShotAsyn(x * scale, y * scale);
                        Driver.clickByCoordinate(point.x, point.y);
                        break;
                    case ConfigUtil.LONG_PRESS_RATIO:
                        //random = Util.internalNextInt(0,size);
                        lpIndex = lpIndex % longPressPointSize;
                        point = longPressPointList.get(lpIndex++);

                        PictureUtil.takeAndModifyScreenShotAsyn(x * scale, y * scale);
                        Driver.LongPressCoordinate(point.x, point.y);
                        break;
                    case ConfigUtil.DOUBLE_TAP_RATIO:
                        PictureUtil.takeAndModifyScreenShotAsyn(x * scale, y * scale);
                        Driver.doubleClickByCoordinate(x, y);
                        break;
                    case ConfigUtil.PINCH_RATIO:
                        PictureUtil.takeAndModifyScreenShotAsyn(pointList,"pinch");
                        Driver.pinch(x,y,endX,endY,false);
                        break;
                    case ConfigUtil.UNPINCH_RATIO:
                        PictureUtil.takeAndModifyScreenShotAsyn(pointList,"unpinch");
                        Driver.pinch(x,y,endX,endY,true);
                        break;
                    case ConfigUtil.DRAG_RATIO:
                        PictureUtil.takeAndModifyScreenShotAsyn(pointList,"drag");
                        Driver.drag(x,y,endX,endY);
                        break;
                    case ConfigUtil.BACK_KEY_RATIO:
                        Driver.pressBack();
                        break;
                    case ConfigUtil.CLICK_ITEM_BY_XPATH_RATIO:
                        if(xpathListSize == 0){
                            log.error("xpath list is 0");
                            break;
                        }

                        log.info("Xpath item list size : " + specialPointSize);
                        log.info("Xpath item list  : " + specialPointList);
                        log.info("xpathIndex is " + xpIndex++);

                        xpIndex = xpIndex % xpathListSize;

                        MobileElement elem = Driver.findElementWithoutException(By.xpath(xpathItemList.get(xpIndex)));
                        if(elem == null){
                            log.error("!!!Element is not found by xpath : " + xpathItemList.get(xpIndex));
                        }else{
                            log.info("Element is found by xpath : " + xpathItemList.get(xpIndex));
                            PictureUtil.takeAndModifyScreenShotAsyn(elem.getCenter().getX() * scale, elem.getCenter().getY() * scale);
                        }

                        break;
                }

                log.info(monkeyEventRatioMap.toString());
                log.info(monkeyEventSummaryRatioMap.toString());

                String xml = Driver.getPageSource(0);
                String packageName=getAppName(xml);

                if (PackageStatus.VALID != isValidPackageName(packageName, ignoreCrash)) {
                    if (!ignoreCrash) {
                        break;
                    }
                }
                if (packageName.equals("com.tencent.mm") || packageName.equals("com.tencent.xin")){
                    if(xml.contains("??????????????????")){
                        log.info("???????????????????????????????????????app");
                        Driver.appRelaunch();
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                log.error("Unexpected error found, continue testing");
            }
        }
    }

    public static String showNodes(String xml, String oldNodePath) throws Exception{
        Document document = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        NodeList nodes = (NodeList) xpath.evaluate(clickXpath, document, XPathConstants.NODESET);

        int length = nodes.getLength();

        log.info(String.valueOf("UI nodes length : " + length));
        String temp = oldNodePath;

        while(--length >=0){
            Node node = nodes.item(length);
            String xpath = getNodeXpath(node);
            if(null != xpath) {
                log.info("getNodeXpath: " + xpath +"\n");
            }
        }

        log.info("!!!!! " + temp);
        return temp;
    }
}




