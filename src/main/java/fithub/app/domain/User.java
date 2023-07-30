package fithub.app.domain;


import fithub.app.domain.common.BaseEntity;
import fithub.app.domain.enums.Gender;
import fithub.app.domain.enums.SocialType;
import fithub.app.domain.enums.Status;
import fithub.app.domain.mapping.ArticleLikes;
import fithub.app.domain.mapping.CommentsLikes;
import fithub.app.domain.mapping.RecordLikes;
import fithub.app.domain.mapping.SavedArticle;
import fithub.app.web.dto.requestDto.UserRequestDto;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Builder
@Getter
@DynamicInsert
@DynamicUpdate
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true ,nullable = true)
    private String email;

    @Column(length = 11, unique = true, nullable = true)
    private String phoneNum;

    @Column(length = 12, unique = true, nullable = true)
    private String nickname;

    private String profileUrl;

    @Column(length = 20, nullable = true)
    private String name;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(nullable = true)
    private Integer age;

    @Column(nullable = true)
    private Boolean isSocial;

    @Enumerated(EnumType.STRING)
    private SocialType socialType;

    private String password;

    @Column(nullable = true)
    private Boolean marketingAgree;

    @Column(columnDefinition = "BIGINT DEFAULT 0")
    private Long totalRecordNum;

    @Column(columnDefinition = "BIGINT DEFAULT 0")
    private Long monthlyRecordNum;

    @Column(columnDefinition = "BIGINT DEFAULT 0")
    private Long contiguousRecordNum;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(10) DEFAULT 'ACTIVE'")
    private Status status;

    private LocalDateTime inactiveDate;

    @Column(columnDefinition = "BIGINT DEFAULT 0")
    private Long reported;

    private String socialId;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Article> articleList = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Record> recordList = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<ArticleLikes> articleLikesList = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<SavedArticle> savedArticleList = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<UserExercise> userExerciseList = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<RecordLikes> recordLikesList = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Comments> commentsList = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<CommentsLikes> commentsLikesList = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "main_exercise")
    private UserExercise mainExercise;

    public User update(String name){
        this.name = name;
        return this;
    }

    public User socialJoin(String email, SocialType socialType){
        this.email = email;
        this.socialType = socialType;
        return this;
    }

    public User setPassword(String password){
        this.password = password;
        return this;
    }

    public User updateInfo(UserRequestDto.UserOAuthInfo request, Integer age, Gender gender, String profileUrl){
        this.name = request.getName();
        this.age = age;
        this.marketingAgree = request.getMarketingAgree();
        this.nickname = request.getNickname();
        this.gender = gender;
        this.profileUrl = profileUrl;
        return  this;
    }

    public Boolean isLikedCommentsFind(Comments comments){
        return this.commentsLikesList.stream()
                .filter(commentsLikes -> commentsLikes.getComments().getId().equals(comments.getId()))
                .collect(Collectors.toList()).size() > 0;
    }

    public Boolean isLikedArticle(Article article){
        return this.articleLikesList.stream()
                .filter(articleLikes -> articleLikes.getArticle().getId().equals(article.getId()))
                .collect(Collectors.toList()).size() > 0;
    }

    public Boolean isLikedRecord(Record record){
        return this.recordLikesList.stream()
                .filter(recordLikes -> recordLikes.getRecord().getId().equals(record.getId()))
                .collect(Collectors.toList()).size() > 0;
    }

    public void setUserExerciseList(List<UserExercise> exerciseList){
        this.userExerciseList = exerciseList;
    }

    public User setMainExercise(UserExercise mainExercise){
        this.mainExercise = mainExercise;
        return this;
    }

    public void addRecordCount(){
        this.monthlyRecordNum = this.monthlyRecordNum + 1;
        this.totalRecordNum = this.totalRecordNum + 1;
    }

    public void addContiguousRecord(){
        this.contiguousRecordNum = this.contiguousRecordNum + 1;
    }

    public void returnContiguousRecord(){
        this.contiguousRecordNum = 0L;
    }
}
