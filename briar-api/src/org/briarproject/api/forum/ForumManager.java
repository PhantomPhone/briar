package org.briarproject.api.forum;

import org.briarproject.api.contact.Contact;
import org.briarproject.api.contact.ContactId;
import org.briarproject.api.db.DbException;
import org.briarproject.api.sync.ClientId;
import org.briarproject.api.sync.GroupId;
import org.briarproject.api.sync.MessageId;

import java.util.Collection;

public interface ForumManager {

	/** Returns the unique ID of the forum client. */
	ClientId getClientId();

	/** Creates a forum with the given name. */
	Forum createForum(String name);

	/** Subscribes to a forum. */
	void addForum(Forum f) throws DbException;

	/** Stores a local forum post. */
	void addLocalPost(ForumPost p) throws DbException;

	/** Returns all forums to which the user could subscribe. */
	Collection<Forum> getAvailableForums() throws DbException;

	/** Returns the forum with the given ID. */
	Forum getForum(GroupId g) throws DbException;

	/** Returns all forums to which the user subscribes. */
	Collection<Forum> getForums() throws DbException;

	/** Returns the body of the forum post with the given ID. */
	byte[] getPostBody(MessageId m) throws DbException;

	/** Returns the headers of all posts in the given forum. */
	Collection<ForumPostHeader> getPostHeaders(GroupId g) throws DbException;

	/** Returns all contacts who subscribe to the given forum. */
	Collection<Contact> getSubscribers(GroupId g) throws DbException;

	/** Returns the IDs of all contacts to which the given forum is visible. */
	Collection<ContactId> getVisibility(GroupId g) throws DbException;

	/** Unsubscribes from a forum. */
	void removeForum(Forum f) throws DbException;

	/** Marks a forum post as read or unread. */
	void setReadFlag(MessageId m, boolean read) throws DbException;

	/**
	 * Makes a forum visible to the given set of contacts and invisible to any
	 * other current or future contacts.
	 */
	void setVisibility(GroupId g, Collection<ContactId> visible)
			throws DbException;

	/**
	 * Makes a forum visible to all current and future contacts, or invisible
	 * to future contacts.
	 */
	void setVisibleToAll(GroupId g, boolean all) throws DbException;
}
