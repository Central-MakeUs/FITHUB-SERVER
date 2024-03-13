package fithub.app.service.converter;

import fithub.app.aws.s3.AmazonS3Manager;
import fithub.app.base.Code;
import fithub.app.base.exception.handler.ArticleException;
import fithub.app.domain.*;
import fithub.app.domain.mapping.ArticleHashTag;
import fithub.app.domain.mapping.ContentsReport;
import fithub.app.repository.ArticleRepositories.ArticleRepository;
import fithub.app.repository.ExerciseCategoryRepository;
import fithub.app.repository.HashTagRepositories.HashTagRepository;
import fithub.app.utils.TimeConverter;
import fithub.app.web.dto.requestDto.ArticleRequestDto;
import fithub.app.web.dto.responseDto.ArticleResponseDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ArticleConverter {

    Logger logger = LoggerFactory.getLogger(ArticleConverter.class);

    private final ArticleRepository articleRepository;

    private final ExerciseCategoryRepository exerciseCategoryRepository;

    private final AmazonS3Manager amazonS3Manager;
    private static ArticleRepository staticArticleRepository;
    private static ExerciseCategoryRepository staticExerciseCategoryRepository;

    private final HashTagRepository hashTagRepository;
    private static AmazonS3Manager staticAmazonS3Manager;

    private static Logger staticLogger;

    private static String pattern = "https://cmc-fithub\\.s3\\.ap-northeast-2\\.amazonaws\\.com(.*)";

    private static HashTagRepository staticHashTagRepository;

    private final TimeConverter timeConverter;

    private static TimeConverter staticTimeConverter;


    @PostConstruct
    public void init() {
        staticArticleRepository = this.articleRepository;
        staticExerciseCategoryRepository = this.exerciseCategoryRepository;
        staticAmazonS3Manager = this.amazonS3Manager;
        staticTimeConverter = this.timeConverter;
        staticLogger = this.logger;
        staticHashTagRepository = this.hashTagRepository;
    }

    public static Article toArticle(ArticleRequestDto.CreateArticleDto request, User user, List<HashTag> hashTagList, Integer categoryId)throws IOException
    {
        ExerciseCategory exerciseCategory= staticExerciseCategoryRepository.findById(categoryId).orElseThrow(()->new ArticleException(Code.CATEGORY_ERROR));
        Article article = Article.builder()
                .title(request.getTitle())
                .contents(request.getContents())
                .user(user)
                .articleHashTagList(new ArrayList<>())
                .articleImageList(new ArrayList<>())
                .exerciseCategory(exerciseCategory)
                .build();

        staticLogger.info("생성된 article : {}", article.toString());
        article.setArticleHashTagList(toArticleHashTagList(hashTagList, article));
        article.setUser(user);

        // 사진 업로드 하기
        List<MultipartFile> articleImageList= request.getPictureList();
        if(articleImageList != null && !articleImageList.isEmpty()){
            createAndMapArticleImage(articleImageList,article);
        }
        return article;
    }

    public static Article toUpdateArticle(Article article,ArticleRequestDto.UpdateArticleDto request, List<HashTag> hashTagList) throws IOException
    {
        ExerciseCategory exerciseCategory = staticExerciseCategoryRepository.findById(request.getCategory()).orElseThrow(() -> new ArticleException(Code.CATEGORY_ERROR));
        article.update(request, exerciseCategory);
        article.setArticleHashTagList(toArticleHashTagList(hashTagList, article));

        List<MultipartFile> articleImageList = request.getNewPictureList();
        if(articleImageList != null && !articleImageList.isEmpty()){
            createAndMapArticleImage(articleImageList,article);
        }
        return article;
    }

    public static void createAndMapArticleImage(List<MultipartFile> articleImageList, Article article) throws IOException
    {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for (int i = 0; i < articleImageList.size(); i++) {
            final MultipartFile image = articleImageList.get(i);

            executor.submit(() -> {
                try {
                    Uuid uuid = staticAmazonS3Manager.createUUID();
                    String keyName = staticAmazonS3Manager.generateArticleKeyName(uuid, image.getOriginalFilename());
                    String fileUrl = staticAmazonS3Manager.uploadFile(keyName, image);
                    staticLogger.info("S3에 업로드 한 파일의 url : {}", fileUrl);

                    // 여기서 ArticleImage 객체 생성과 관련된 작업은 동기화 이슈를 피하기 위해 주의 깊게 처리해야 합니다.
                    synchronized (article) {
                        ArticleImage articleImage = ArticleImageConverter.toArticleImage(fileUrl, article, uuid);
                        articleImage.setArticle(article);
                        // 필요하다면 여기에서 articleImage를 저장하거나 관리하는 로직을 추가합니다.
                    }
                } catch (IOException e) {
                    staticLogger.error("파일 업로드 에러 발생", e);
                }
            });
        }

        executor.shutdown();
        try {
            // 모든 스레드 작업이 완료될 때까지 대기
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
//        ExecutorService executorService = Executors.newFixedThreadPool(Math.min(articleImageList.size(), 5));
//        List<CompletableFuture<Void>> futures = articleImageList.stream()
//                .map((image) -> CompletableFuture.runAsync(() -> {
//
//                }, executorService))
//                .collect(Collectors.toList());
    }

    private static List<ArticleHashTag> toArticleHashTagList(List<HashTag> hashTagList, Article article){
        return hashTagList.stream()
                .map(hashTag -> {
                    ArticleHashTag articleHashTag = ArticleHashTag.builder().build();
                    articleHashTag.setArticle(article);
                    articleHashTag.setHashTag(hashTag);
                    return articleHashTag;
                }).collect(Collectors.toList());
    }

    public static ArticleResponseDto.ArticleSpecDto toArticleSpecDto(Article article, User user, ExerciseCategory exerciseCategory){

        String name = exerciseCategory.getName();
        Optional<HashTag> exerciseHashTagOptional = staticHashTagRepository.findByName(name);

        HashTag hashTag = exerciseHashTagOptional.get();

        return ArticleResponseDto.ArticleSpecDto.builder()
                .articleId(article.getId())
                .articleCategory(ExerciseCategoryConverter.toCategoryDto(article.getExerciseCategory()))
                .loginUserProfileUrl(user.getProfileUrl())
                .userInfo(UserConverter.toCommunityUserInfo(article.getUser()))
                .title(article.getTitle())
                .contents(article.getContents())
                .comments(staticArticleRepository.countComments(article, user, user))
                .articlePictureList(PictureConverter.toPictureDtoList(article.getArticleImageList()))
                .createdAt(staticTimeConverter.convertTime(article.getCreatedAt()))
                .Hashtags(HashTagConverter.toHashtagDtoList(article.getArticleHashTagList(), hashTag))
                .likes(staticArticleRepository.countLikes(article,user,user))
                .scraps(staticArticleRepository.countScraps(article,user,user))
                .isLiked(user.isLikedArticle(article))
                .isScraped(user.isSavedArticle(article))
                .build();
    }

    public static ArticleResponseDto.ArticleDto toArticleDto(Article article, User user, Boolean isAll){
        return ArticleResponseDto.ArticleDto.builder()
                .articleId(article.getId())
                .userInfo(UserConverter.toCommunityUserInfo(article.getUser()))
                .articleCategory(ExerciseCategoryConverter.toCategoryDto(article.getExerciseCategory()))
                .title(article.getTitle())
                .contents(article.getContents())
                .pictureUrl(article.getArticleImageList().size() == 0 ? null : article.getArticleImageList().get(0).getImageUrl())
                .exerciseTag(!isAll ? null : article.getExerciseCategory().getName())
                .likes(staticArticleRepository.countLikes(article, user,user))
                .comments(staticArticleRepository.countComments(article,user,user))
                .isLiked(user.isLikedArticle(article))
                .createdAt(staticTimeConverter.convertTime(article.getCreatedAt()))
                .build();
    }

    public static ArticleResponseDto.ArticleDtoList toArticleDtoList(Page<Article> articleList, User user, Boolean isAll){
        List<ArticleResponseDto.ArticleDto> articleDtoList =
                articleList.stream()
                        .map(article -> toArticleDto(article, user, isAll))
                        .collect(Collectors.toList());

        return ArticleResponseDto.ArticleDtoList.builder()
                .articleList(articleDtoList)
                .listSize(articleDtoList.size())
                .totalElements(articleList.getTotalElements())
                .totalPage(articleList.getTotalPages())
                .isFirst(articleList.isFirst())
                .isLast(articleList.isLast())
                .build();
    }

    public static ArticleResponseDto.ArticleCreateDto toArticleCreateDto(Article article){
        return ArticleResponseDto.ArticleCreateDto.builder()
                .articleId(article.getId())
                .title(article.getTitle())
                .ownerId(article.getUser().getId())
                .createdAt(article.getCreatedAt())
                .build();
    }

    public static ArticleResponseDto.ArticleUpdateDto toArticleUpdateDto(Article article){
        return ArticleResponseDto.ArticleUpdateDto.builder()
                .articleId(article.getId())
                .updatedAt(article.getUpdatedAt())
                .build();
    }

    public static ArticleResponseDto.ArticleDeleteDto toArticleDeleteDto(Long id){
        return ArticleResponseDto.ArticleDeleteDto.builder()
                .articleId(id)
                .deletedAt(LocalDateTime.now())
                .build();
    }

    public static ArticleResponseDto.ArticleDeleteDtoList toArticleDeleteDtoList(List<Long> idList){

        List<ArticleResponseDto.ArticleDeleteDto> articleDeleteDtoList =
                idList.stream()
                        .map(id -> toArticleDeleteDto(id))
                        .collect(Collectors.toList());

        return ArticleResponseDto.ArticleDeleteDtoList.builder()
                .deletedArticleList(articleDeleteDtoList)
                .size(articleDeleteDtoList.size())
                .build();
    }

    public static ArticleResponseDto.ArticleLikeDto toArticleLikeDto(Article article, User user){
        return ArticleResponseDto.ArticleLikeDto.builder()
                .articleId(article.getId())
                .articleLikes(staticArticleRepository.countLikes(article, user,user))
                .isLiked(user.isLikedArticle(article))
                .build();
    }

    public static ArticleResponseDto.ArticleSaveDto toArticleSaveDtoDto(Article article, User user){
        return ArticleResponseDto.ArticleSaveDto.builder()
                .articleId(article.getId())
                .articleSaves(article.getSaves())
                .isSaved(user.isSavedArticle(article))
                .build();
    }

    public static String toKeyName(String imageUrl) {
        String input = imageUrl;

        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(input);
        String extractedString = null;
        if (matcher.find())
            extractedString = matcher.group(1);

        return extractedString;
    }

    public static ArticleResponseDto.ArticleReportDto toArticleReportDto(ContentsReport articleReport, Long articleId){
        return ArticleResponseDto.ArticleReportDto.builder()
                .reportedArticleId(articleId)
                .reportedAt(LocalDateTime.now())
                .build();
    }

    public static ArticleResponseDto.ArticleRecommendKeywordDto toArticleRecommendKeywordDto(List<RecommendArticleKeyword> articleKeywordList){
        List<String> stringList = articleKeywordList.stream()
                .map(recommendArticleKeyword -> recommendArticleKeyword.getKeyword()).collect(Collectors.toList());

        return ArticleResponseDto.ArticleRecommendKeywordDto.builder()
                .keywordList(stringList)
                .size(stringList.size())
                .build();
    }
}
