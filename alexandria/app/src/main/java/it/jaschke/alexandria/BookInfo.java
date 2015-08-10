package it.jaschke.alexandria;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import it.jaschke.alexandria.data.AlexandriaContract;

/**
 * Created by joshua on 8/10/15.
 */
public class BookInfo implements Parcelable {

    private String title;
    private String subTitle;
    private String imageUrl;
    private String authors;
    private String categories;

    public BookInfo(Cursor data) {
        title = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        subTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
        authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));
        imageUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));
        categories = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));
    }

    protected BookInfo(Parcel in) {
        title = in.readString();
        subTitle = in.readString();
        imageUrl = in.readString();
        authors = in.readString();
        categories = in.readString();
    }

    public static final Creator<BookInfo> CREATOR = new Creator<BookInfo>() {
        @Override
        public BookInfo createFromParcel(Parcel in) {
            return new BookInfo(in);
        }

        @Override
        public BookInfo[] newArray(int size) {
            return new BookInfo[size];
        }
    };

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public String getCategories() {
        return categories;
    }

    public void setCategories(String categories) {
        this.categories = categories;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(title);
        parcel.writeString(subTitle);
        parcel.writeString(imageUrl);
        parcel.writeString(authors);
        parcel.writeString(categories);
    }
}
