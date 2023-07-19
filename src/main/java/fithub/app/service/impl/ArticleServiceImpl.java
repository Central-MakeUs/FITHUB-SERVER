package fithub.app.service.impl;

import fithub.app.aws.s3.AmazonS3Manager;
import fithub.app.base.Code;
import fithub.app.base.exception.handler.ArticleException;
import fithub.app.converter.ArticleConverter;
import fithub.app.converter.HashTagConverter;
import fithub.app.domain.Article;
import fithub.app.domain.ArticleImage;
import fithub.app.domain.HashTag;
import fithub.app.domain.User;
import fithub.app.domain.mapping.ArticleHashTag;
import fithub.app.domain.mapping.ArticleLikes;
import fithub.app.domain.mapping.SavedArticle;
import fithub.app.repository.ArticleRepositories.*;
import fithub.app.repository.HashTagRepository;
import fithub.app.service.ArticleService;
import fithub.app.web.dto.requestDto.ArticleRequestDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleServiceImpl implements ArticleService {

    Logger logger = LoggerFactory.getLogger(ArticleServiceImpl.class);

    private final ArticleRepository articleRepository;
    private final HashTagRepository hashTagRepository;

    private final ArticleLikesRepository articleLikesRepository;

    private final SavedArticleRepository savedArticleRepository;

    private final ArticleImageRepository articleImageRepository;

    private final ArticleHashTagRepository articleHashTagRepository;

    private final AmazonS3Manager amazonS3Manager;

    @Override
    @Transactional(readOnly = false)
    public Article create(ArticleRequestDto.CreateArticleDto request, User user, Integer categoryId) throws IOException
    {
        String exerciseTag = request.getExerciseTag();
        HashTag exercisehashTag = hashTagRepository.findByName('#' + exerciseTag).orElseGet(() -> HashTagConverter.newHashTag(exerciseTag));
        List<HashTag> hashTagList = request.getTagList().stream()
                .map(tag -> hashTagRepository.findByName('#' + tag).orElseGet(()-> HashTagConverter.newHashTag(tag)))
                .collect(Collectors.toList());

        hashTagList.add(exercisehashTag);
        Article article = ArticleConverter.toArticle(request, user, hashTagList, categoryId);
        return articleRepository.save(article);
    }

    @Override
    public Article getArticle(Long ArticleId) {
        return articleRepository.findById(ArticleId).orElseThrow(()-> new ArticleException(Code.ARTICLE_NOT_FOUND));
    }

    @Override
    public Boolean getIsSaved(Article article, User user){
        Optional<SavedArticle> isSaved = savedArticleRepository.findByArticleAndUser(article, user);
        return isSaved.isPresent();
    }

    @Override
    public Boolean getIsLiked(Article article, User user){
        Optional<ArticleLikes> isLiked = articleLikesRepository.findByArticleAndUser(article, user);
        return isLiked.isPresent();
    }

    @Override
    @Transactional(readOnly = false)
    public Article toggleArticleLike(Long articleId, User user) {
        Article article = articleRepository.findById(articleId).orElseThrow(() -> new ArticleException(Code.ARTICLE_NOT_FOUND));
        Optional<ArticleLikes> articleLike = articleLikesRepository.findByArticleAndUser(article, user);

        Article updatedArticle;

        if(articleLike.isPresent()){
            articleLike.get().getUser().getArticleLikesList().remove(articleLike.get());
            articleLikesRepository.delete(articleLike.get());
            updatedArticle = article.likeToggle(false);
        }else{
            updatedArticle = article.likeToggle(true);
            ArticleLikes articleLikes = ArticleLikes.builder()
                    .article(updatedArticle)
                    .user(user)
                    .build();
            articleLikes.setUser(user);
            articleLikesRepository.save(articleLikes);
        }

        return updatedArticle;
    }

    @Override
    @Transactional(readOnly = false)
    public Article toggleArticleSave(Long articleId, User user) {
        Article article = articleRepository.findById(articleId).orElseThrow(() -> new ArticleException(Code.ARTICLE_NOT_FOUND));
        Optional<SavedArticle> savedArticle = savedArticleRepository.findByArticleAndUser(article, user);

        Article updatedArticle;
        if(savedArticle.isPresent()){
            savedArticle.get().getUser().getArticleLikesList().remove(savedArticle.get());
            savedArticleRepository.delete(savedArticle.get());
            updatedArticle = article.saveToggle(false);
        }else{
            updatedArticle = article.saveToggle(true);
            SavedArticle newSavedArticle = SavedArticle.builder()
                    .article(article)
                    .user(user)
                    .build();
            newSavedArticle.setUser(user);
            savedArticleRepository.save(newSavedArticle);
        }

        return updatedArticle;
    }

    @Override
    @Transactional(readOnly = false)
    public Article updateArticle(Long articleId, ArticleRequestDto.UpdateArticleDto request, User user) throws IOException
    {
        Article article = articleRepository.findById(articleId).orElseThrow(() -> new ArticleException(Code.ARTICLE_NOT_FOUND));

        if(!article.getUser().getId().equals(user.getId()))
            throw new ArticleException(Code.ARTICLE_FORBIDDEN);

        List<String> images = article.getArticleImageList().stream()
                .map(articleImage -> articleImage.getImageUrl())
                .collect(Collectors.toList());

        List<String> deleteTarget = images.stream()
                .filter(image -> !request.getRemainPictureUrlList().contains(image))
                .collect(Collectors.toList());

        for(int i = 0; i < deleteTarget.size(); i++)
            logger.info("삭제할 사진 : {}", deleteTarget.get(i));

        for(int i = 0; i < deleteTarget.size(); i++) {
            String s = deleteTarget.get(i);
            ArticleImage articleImage = articleImageRepository.findByImageUrl(s).get();
            article.getArticleImageList().remove(articleImage);
            articleImageRepository.delete(articleImage);
            String Keyname = ArticleConverter.toKeyName(deleteTarget.get(i));
            amazonS3Manager.deleteFile(Keyname.substring(1));
        }

        // 필요 없어진 사진은 지웠으니 이제 게시글과 연결된 해시태그 재 설정
        List<ArticleHashTag> articleHashTagList = article.getArticleHashTagList();

        for(int i = 0; i < articleHashTagList.size(); i++) {
            ArticleHashTag articleHashTag = articleHashTagList.get(i);
            article.getArticleHashTagList().remove(articleHashTag);
            articleHashTagRepository.delete(articleHashTag);
        }

        if (articleHashTagList.size() > 0) {
            ArticleHashTag last = articleHashTagList.get(0);
            articleHashTagList.remove(last);
            articleHashTagRepository.delete(last);
        }

        String exerciseTag =  request.getExerciseTag();
        HashTag exercisehashTag = hashTagRepository.findByName('#' + exerciseTag).orElseGet(() -> HashTagConverter.newHashTag(exerciseTag));

        List<HashTag> hashTagList = request.getHashTagList().stream()
                .map(tag -> hashTagRepository.findByName('#' + tag).orElseGet(()-> HashTagConverter.newHashTag(tag)))
                .collect(Collectors.toList());

        hashTagList.add(exercisehashTag);

        return ArticleConverter.toUpdateArticle(article,request,hashTagList);
    }

    @Override
    @Transactional(readOnly = false)
    public void deleteArticleSingle(Long articleId, User user) {
        Article article = articleRepository.findById(articleId).orElseThrow(() -> new ArticleException(Code.ARTICLE_NOT_FOUND));

        if(!article.getUser().getId().equals(user.getId()))
            throw new ArticleException(Code.ARTICLE_FORBIDDEN);

        articleRepository.delete(article);
    }
}
