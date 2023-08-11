package com.bitc.full505_final_team4.service;

import com.bitc.full505_final_team4.common.JsonUtils;
import com.bitc.full505_final_team4.common.WebDriverUtil;
import com.bitc.full505_final_team4.data.dto.NovelMainDto;
import lombok.RequiredArgsConstructor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.propertyeditors.CurrencyEditor;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class NovelMainServiceImpl implements NovelMainService{

  /* 리디북스 카테고리 별 순위 리스트 불러오기
  * 시작번호로부터 20개 출력
  * 총 100위까지 출력되도록 함 */
  @Override
  public List<NovelMainDto> getRidiRankList(String category, int startNum) throws Exception {

    List<NovelMainDto> novelDtoList = new ArrayList<>();

    String url = "https://api.ridibooks.com/v2/bestsellers?category_includes=";
      url += category + "&offset=";
      url += (startNum - 1) + "&limit=20&period=DAILY";

    // 페이지 json 객체로 변환
    JSONObject novelResult = (JSONObject) JsonUtils.jsonUrlParser(url).get("data");
    ArrayList<JSONObject> novelList = (ArrayList<JSONObject>) novelResult.get("items"); // 배열 변환

    if (!novelList.isEmpty()){
      for(JSONObject novelItem : novelList){
        // 각각 item에 들어있는 book Object
        HashMap<String, Object > book = (HashMap<String, Object>) novelItem.get("book");

        // 값을 저장할 dto
        NovelMainDto novel = new NovelMainDto();

        // 소설 순위
        novel.setNovelIndexNum(novelList.indexOf(novelItem) + 1); // novelItem의 인덱스 번호

        // 플랫폼 제공 아이디
        novel.setPlatformId(book.get("book_id").toString());

        // 소설 제목 얻어오기
        JSONObject serial = (JSONObject) book.get("serial");
        novel.setNovelTitle(serial.get("title").toString());

        // 작가 이름 얻어오기
        ArrayList authorsList = (ArrayList) book.get("authors");
        HashMap<String, Object> authors = (HashMap<String, Object>) authorsList.get(0);
        novel.setNovelAuthor(authors.get("name").toString());

        // 소설 썸네일
        JSONObject cover = (JSONObject) book.get("cover");
        novel.setNovelThumbnail(cover.get("large").toString());

        // 소설 카테고리
        JSONArray categories = (JSONArray) book.get("categories");
        JSONObject categoryFirst = (JSONObject) categories.get(0);
        novel.setCateList(categoryFirst.get("name").toString());

        // 소설 별점
        JSONArray ratings = (JSONArray) book.get("ratings");
        novel.setNovelStarRate(getStarRate(ratings)); // 하위에 구현한 함수 사용

        // 성인 여부
        novel.setAdultsOnly((Boolean) book.get("adults_only"));

        novelDtoList.add(novel);
      }

      return novelDtoList;
    }

    return null;
  }

  @Override
  public List<NovelMainDto> getKakaoList(String urlId) throws Exception {

    // 크롤링 할 WebDriverUtil 객체 생성
    WebDriver driver = WebDriverUtil.getChromeDriver();

    List<NovelMainDto> novelDtoList = new ArrayList<>();

    // 카카오 웹소설 실시간 랭킹 주소
    String url = "https://page.kakao.com/menu/10011/screen/" + urlId;

    if (!ObjectUtils.isEmpty(driver)){
      driver.get(url);

      try {
        // 브라우저 이동 시 생기는 로드시간을 기다린다.
        // HTTP 응답속도보다 자바의 컴파일 속도가 더 빠르기 때문에 임의적으로 1초를 대기한다
//        Thread.sleep(1000);
        driver.manage().timeouts().implicitlyWait(2, TimeUnit.SECONDS);
      } catch (Exception e){
        e.printStackTrace();
      }

    }

    try {
      //카카오 웹소설 랭킹 리스트 들고오기
//      WebElement element = driver.findElement(By.cssSelector(".foldable:grid-inner-border-cols6"));
      List<WebElement> list = driver.findElements(By.xpath("//div[@style='border-color:transparent;border-width:4px']")).subList(0, 12);
      // list = list.subList(0, 20);// 20개 리스트 들고오기

      if(!ObjectUtils.isEmpty(list)){ // list 가 null 값이 아니라면
        for(WebElement element : list){
          /* dto 에 필수적으로 넣어야 하는 요소
          * 플랫폼 아이디, 제목, 썸네일 주소, 플랫폼 이름 */
          NovelMainDto novel = new NovelMainDto();

          novel.setPlatform("kakao"); // 플랫폼 이름 입력

          //순위 불러오기
          novel.setNovelIndexNum(list.indexOf(element) + 1); // novelItem의 인덱스 번호

          // a 태그에서 id 값 잘라오기
          String id = element.findElement(By.tagName("a")).getAttribute("href");
          int idIdx = id.lastIndexOf("/")+1;
          id = id.substring(idIdx);

          novel.setPlatformId(id);

          // 제목
          String title = element.findElement(By.cssSelector(".line-clamp-2")).getText();
          novel.setNovelTitle(title);

//          String locator = "//img[@alt=\"썸네일\"][" + list.indexOf(element) + "]";
          //썸네일
          String thumbnail = element.findElement(By.cssSelector(".object-cover.visible")).getAttribute("src");

          if (ObjectUtils.isEmpty(thumbnail)){
            novel.setAdultsOnly(true);
          } else {
            novel.setNovelThumbnail(thumbnail);
          }

          novelDtoList.add(novel);
        }
      }

      /*
      // element 리스트 내 아이디를 담을 배열
      List<String> idList = new ArrayList<>();

      for(WebElement link : list){
        String id = link.getAttribute("href");

        for (int i = 0; i < 2; i++) {
          // 아이디 값만 잘라오기, "/"가 두개라 두번 돌림
          id = id.substring(id.lastIndexOf("/") + 1);
        }

        idList.add(id); // 리스트에 추가
      }

      // 아이디에 해당하는 작품 정보 리스트 들고오기
      for(String novelId : idList){
        NovelMainDto novel = getKakaoNovel(novelId);
        novelDtoList.add(novel);
      }*/
    }
    catch (Exception e){
      e.printStackTrace();
    }
    finally {
      driver.quit();
    }

    return novelDtoList;
  }

  /* 카카오 특정 작품 정보 가져오기 , selenium 방식 */
  @Override
  public NovelMainDto getKakaoNovel(String novelId) throws Exception {

    // 크롤링 할 WebDriverUtil 객체 생성
    WebDriver driver = WebDriverUtil.getChromeDriver();

    NovelMainDto novel = new NovelMainDto();


    String url = "https://page.kakao.com/content/" + novelId;

    driver.get(url);

    if (!ObjectUtils.isEmpty(driver)){
      driver.get(url);

      try {
        // 브라우저 이동 시 생기는 로드시간을 기다린다.
        // HTTP 응답속도보다 자바의 컴파일 속도가 더 빠르기 때문에 임의적으로 1초를 대기한다.
        Thread.sleep(500);
      } catch (InterruptedException e){
        e.printStackTrace();
      }

    }

    try {

      // 작품 제목
      String title = driver.findElement(By.xpath("//meta[@property='og:title']")).getAttribute("content");
      novel.setNovelTitle(title);

      // 작가 이름
      String author = driver.findElement(By.xpath("//meta[@name='author']")).getAttribute("content");
      novel.setNovelAuthor(author);

      // 썸네일 주소
      String thumbnail = driver.findElement(By.xpath("//meta[@property='og:image']")).getAttribute("content");
      novel.setNovelThumbnail(thumbnail);

      WebElement infoTag = driver.findElement(By.cssSelector(".mt-16pxr.text-el-60"));

      // 장르
      String cate = infoTag.findElements(By.tagName("span")).get(3).getText();
      novel.setCateList(cate);

      // 별점
      String starRate = infoTag.findElements(By.tagName("span")).get(1).getText();
      starRate = String.valueOf(Double.parseDouble(starRate)/2);
      novel.setNovelStarRate(starRate);


      // 작품설명 메타태그로 들고올 수 있음, 필요시 추가
    } catch (Exception e){
      e.printStackTrace();
    }
    finally {
      driver.quit();
    }

    return novel;
  }


  /* Ridi Json 에서 들고온 ratings 별점으로 변환하기 (10점 만점 기준) */
  @Override
  public String getStarRate(JSONArray ratings) throws Exception {

    /* 계산식
    * ((1점 * 1점 count) + ... + (5점 * 5점 count)) / totalCount
    * */

    String starRate = "";

    double multiRating = 0;
    double totalCount = 0;

    /* count * rating 총합 구하기 */

    for (int i = 0; i < ratings.size(); i++) {

      JSONObject rateObject = (JSONObject) ratings.get(i);
      int rating = Integer.parseInt(rateObject.get("rating").toString()) ;
      int count = Integer.parseInt(rateObject.get("count").toString()) ;

      int multiRate = rating * count;

      multiRating += multiRate;
      totalCount += count;

    }

    double total = (multiRating / totalCount) * 2;
    // 왜 한자리수 올림이 안되는 건지...
//    total = (double) Math.ceil((total * 100) / 100.0);

    // 소수점 한자리까지 보여주는 별점 반환
    starRate = String.format("%.1f", total);

    return starRate;
  }
}

